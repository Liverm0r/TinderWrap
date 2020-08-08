package com.livermor.tinderwrap.ui.screen.swap

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.livermor.tinderwrap.databinding.ActivityMainBinding
import com.livermor.tinderwrap.ui.adapter.BioAdapter
import com.livermor.tinderwrap.ui.adapter.PhotoAdapter
import com.livermor.tinderwrap.ui.adapter.SwipeCallback
import com.livermor.tinderwrap.ui.screen.Message
import kotlinx.android.synthetic.main.activity_main.*

class SwapActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val compositeAdapter = CompositeDelegateAdapter(PhotoAdapter(), BioAdapter())

    private val viewModel: SwapViewModel by lazy {
        val factory = SwapViewModel.Factory(applicationContext)
        ViewModelProvider(this, factory).get(SwapViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding) {
            setContentView(root)

            rvPhotos.run {
                layoutManager = LinearLayoutManager(this@SwapActivity)
                adapter = compositeAdapter
                val swipeCallback = SwipeCallback(viewModel::update)
                val itemTouchHelper = ItemTouchHelper(swipeCallback)
                itemTouchHelper.attachToRecyclerView(this)
            }
            bNo.setOnClickListener { viewModel.update(SwapMessage.Choose(like = false)) }
            bYes.setOnClickListener { viewModel.update(SwapMessage.Choose(like = true)) }
        }
        observe(viewModel)
    }

    private fun observe(model: SwapViewModel) {
        model.feed.observe(this, Observer { compositeAdapter.swapData(it) })
        model.errors.observe(this, Observer { toast(it.toString()) })
        model.noMoreAccounts.observe(this, Observer { toast(it.toString()) })
        model.age.observe(this, Observer { tvBirth.text = it.toString() })
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}
