package com.example.store.feedback

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.store.BaseActivity
import com.example.store.R
import com.example.store.databinding.ActivityFeedbackBinding
import kotlinx.coroutines.launch

class FeedbackActivity : BaseActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.closeButton.setOnClickListener {
            finish()
        }

        binding.feedbackInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                binding.charCountText.text = "$len / 1000"
                if (binding.errorContainer.visibility == View.VISIBLE) {
                    binding.errorContainer.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.sendFeedbackButton.setOnClickListener {
            submitFeedback()
        }
    }

    private fun submitFeedback() {
        if (isSubmitting) return

        val text = binding.feedbackInput.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) {
            binding.errorMessageText.text = getString(R.string.feedback_empty_error)
            binding.errorContainer.visibility = View.VISIBLE
            return
        }

        isSubmitting = true
        binding.sendFeedbackButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE

        lifecycleScope.launch {
            val result = FeedbackClient.submitFeedback(this@FeedbackActivity, text)
            isSubmitting = false
            binding.progressBar.visibility = View.GONE
            binding.sendFeedbackButton.isEnabled = true

            when (result) {
                is FeedbackResult.Success -> {
                    binding.formCard.visibility = View.GONE
                    binding.successCard.visibility = View.VISIBLE
                }
                is FeedbackResult.NoInternet -> {
                    binding.errorMessageText.text = getString(R.string.feedback_error_message)
                    binding.errorContainer.visibility = View.VISIBLE
                }
                is FeedbackResult.Error -> {
                    binding.errorMessageText.text = getString(R.string.feedback_error_message)
                    binding.errorContainer.visibility = View.VISIBLE
                }
            }
        }
    }
}
