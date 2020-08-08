package com.livermor.tinderwrap.ui.screen.swap

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livermor.tinderwrap.Bio
import com.livermor.tinderwrap.Estimated
import com.livermor.tinderwrap.LikeResponse
import com.livermor.tinderwrap.UiPhoto
import com.livermor.tinderwrap.UiUser
import com.livermor.tinderwrap.data.ApiFactory
import com.livermor.tinderwrap.data.AppDb
import com.livermor.tinderwrap.data.BioRepository
import com.livermor.tinderwrap.data.PhotoRepository
import com.livermor.tinderwrap.data.TinderApi
import com.livermor.tinderwrap.factory.NamesImpl
import com.livermor.tinderwrap.factory.swipe
import com.livermor.tinderwrap.ui.screen.Message
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.floor

class SwapViewModel(
    private val api: TinderApi,
    private val photoRepository: PhotoRepository,
    private val bioRepository: BioRepository
) : ViewModel() {

    private var users = persistentListOf<UiUser>()
    val feed = MutableLiveData<PersistentList<Estimated>>()
    val noMoreAccounts = MutableLiveData<Boolean>()
    val errors = MutableLiveData<Exception>()
    val age = MutableLiveData<String>()

    init {
        viewModelScope.launch { postNextUser() }
    }

    fun update(msg: Message) {
        val estimated = requireNotNull(feed.value)
        when (msg) {
            is Message.Choose -> {
                viewModelScope.launch {
                    val resp: LikeResponse = when {
                        msg.like -> api.like(users.first().id)
                        else -> api.hate(users.first().id)
                    }
                    Log.i(SwapViewModel::class.java.simpleName, "update: response $resp")

                    photoRepository.savePhotos(estimated.filterIsInstance(UiPhoto::class.java))
                    bioRepository.saveBio(estimated.first() as Bio)

                    users = users.removeAt(0)
                    postNextUser()
                }
            }
            is Message.Swipe -> {
                var currentPhotos = estimated
                val updatedPhoto = currentPhotos[msg.position].swipe(isLeft = msg.isLeft) // O(1)
                currentPhotos = currentPhotos.set(msg.position, updatedPhoto) // O(1)
                feed.value = currentPhotos
            }
        }
    }

    private suspend fun postNextUser() {
        if (users.isEmpty()) requestUsers()
        if (users.isEmpty()) {
            noMoreAccounts.postValue(true)
        } else {
            users.first().let { user ->
                age.postValue(user.birthDate?.toAge() ?: "unknown")
                val items = (user.photos as PersistentList<Estimated>).add(0, Bio(user.bio))
                feed.postValue(items)
            }
        }
    }

    private suspend fun requestUsers() = try {
        users = api.getUsers().data.results.map { userObject ->
            userObject.user.run {
                val uiPhotos = photos.map { UiPhoto(id = it.id, url = it.url) }.toPersistentList()
                UiUser(_id, bio, photos = uiPhotos, birthDate = birth_date)
            }
        }.toPersistentList()
    } catch (e: Exception) {
        errors.postValue(e)
    }

    private fun Date.toAge(): String {
        val now = Date()
        val timeBetween = now.time - time
        val yearsBetween = timeBetween / 3.15576e+10
        return "age: ${floor(yearsBetween).toInt()}"
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SwapViewModel::class.java))
            val api = ApiFactory(AppDb.token).get()
            val photoRepo = PhotoRepository(context, NamesImpl)
            val bioRepo = BioRepository(context, NamesImpl)
            return SwapViewModel(api, photoRepo, bioRepo) as T
        }
    }
}