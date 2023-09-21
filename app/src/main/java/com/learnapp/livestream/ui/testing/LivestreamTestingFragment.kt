package com.learnapp.livestream.ui.testing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.learnapp.livestream.data.models.UserMetaDataDto
import com.learnapp.livestream.databinding.FragmentLivestreamTestingBinding
import com.learnapp.livestream.ui.player.fragment.LivestreamFragment
import timber.log.Timber

class LivestreamTestingFragment : LivestreamFragment() {

    companion object {
        private const val TAG = "LivestreamTestingFragment"
    }

    private var _fragmentBinding: FragmentLivestreamTestingBinding? = null
    private val fragmentBinding get() = _fragmentBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentLivestreamTestingBinding.inflate(
            layoutInflater,
            container,
            false
        )

        val gson = Gson()
        val userMetaDataDto = gson.toJson(
            UserMetaDataDto(
                "username",
                "avatarUrl",
                "68774ec8-556d-4f61-af97-5e4eaf6ea586",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOiJkODZjMmJkZC1mMjIxLTQ4YjAtOWI0ZS05ZWUxNThmNDUyZWEiLCJpcCI6IjI3Ljk3LjEyLjE5NCwgMTMwLjE3Ni4xMDQuMTQ2IiwiY291bnRyeSI6IklOIiwiaWF0IjoxNjg0MTMwMTIwLCJleHAiOjE2ODQ3MzQ5MjAsImF1ZCI6ImxlYXJuYXBwIiwiaXNzIjoiaHlkcmE6MC4wLjEifQ.PPUJCzt00OY9B4HtwzT6C3dHNyHCGOGUQTUIvPPKrg4",
                "ZmtFWfKS9aXK3NZQ2dY8Fbd6KqjF8PDu",
                "https://catalog.prod.learnapp.com",
                "fdfc7d68-cd54-4533-857a-14399a967b34",
                "1ef9120b-4bf5-4f34-adfc-995e66fb5fd8"
            )
        )
        Timber.tag(TAG).d("configLivestream()")
        /*configLivestream(
            fragmentBinding.livestreamView,
            userMetaDataDto
        )*/
        super.onCreateView(inflater, container, savedInstanceState)
        return fragmentBinding.root
    }
}
