package com.learnapp.livestream.ui.player.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.learnapp.livestream.databinding.ViewLivestreamBinding

class LivestreamView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var _binding: ViewLivestreamBinding? = null
    val livestreamBinding get() = _binding!!

    init {
        _binding = ViewLivestreamBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }
}
