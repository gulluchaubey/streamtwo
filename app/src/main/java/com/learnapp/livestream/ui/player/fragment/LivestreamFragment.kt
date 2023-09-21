package com.learnapp.livestream.ui.player.fragment

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.learnapp.livestream.R
import com.learnapp.livestream.data.models.TrackPeerMap
import com.learnapp.livestream.data.network.remote.Resource
import com.learnapp.livestream.data.preferences.UserSharedPreference
import com.learnapp.livestream.databinding.ViewLivestreamBinding
import com.learnapp.livestream.ui.base.BaseFragment
import com.learnapp.livestream.ui.player.adapter.PeerScreenAdapter
import com.learnapp.livestream.ui.player.model.ApiErrorCodes
import com.learnapp.livestream.ui.player.model.NetworkQualityCode
import com.learnapp.livestream.ui.player.model.RoomStatus
import com.learnapp.livestream.ui.player.view.LivestreamView
import com.learnapp.livestream.ui.player.viewmodel.PlayerViewModel
import com.learnapp.livestream.utils.handleApiError
import com.learnapp.livestream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class LivestreamFragment : BaseFragment(), View.OnClickListener {

    @Inject
    lateinit var userSharedPreference: UserSharedPreference

    companion object {
        private const val TAG = "LivestreamFragment"
        private const val CONNECTION_ONLINE_DELAY = 2000L
        private const val ROOM_STATUS_DELAY = 1000L
        private const val DEFAULT_SPAN_COUNT = 1
        private const val MAX_SPAN_COUNT = 2
        private const val UN_READ_MESSAGE_COUNT_THRESHOLD = 9
        private const val DEFAULT_UN_READ_COUNT = 0
        private const val PLAYER_HEIGHT_DEFAULT = 0
    }

    private val viewModel: PlayerViewModel by activityViewModels()
    private var livestreamBinding: ViewLivestreamBinding? = null
    lateinit var mContext: Application

    private var peerAdapter: PeerScreenAdapter? = null
    private var gridLayoutManager: GridLayoutManager? = null

    private var peerCount: Int = 0

    private var livestreamHeight: Int? = null

    /**
     * Do Not Delete this fuction.It is being called in main application.
     */
    fun configLivestream(
        livestreamView: LivestreamView,
        userMetaDataDto: String,
        application: Application
    ) {
        Timber.tag(TAG).v(userMetaDataDto)
        Timber.tag(TAG + "configLivestream").v(userMetaDataDto)
        this.livestreamBinding = livestreamView.livestreamBinding
        this.mContext = application
        if (extractData(userMetaDataDto)) {
            setBaseViewModel(viewModel)
            setListenersAndObservers(application)
        }
    }

    private fun setListenersAndObservers(application: Application) {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        initViewVisibility()
        setOnClickListener()
        setObserver()
        checkLiveStream(application)
        initPeerAdapter()
        toggleSystemBars(false)
    }

    private fun initViewVisibility() {
        Timber.tag(TAG).v("initViewVisibility()")
        livestreamBinding?.let { livestreamBinding ->
            livestreamBinding.viewGroup.visible(false)
            livestreamBinding.connectivityTextView.visible(false)
            livestreamBinding.statStreamTextView.visible(false)
            setUnReadMessageCount(viewModel.unreadMessagesCount.value ?: DEFAULT_UN_READ_COUNT)
        }
    }

    private fun checkLiveStream(application: Application) {
        try {
            Timber.tag(TAG).v("checkLiveStream")
            viewModel.initLiveStream(application)
        } catch (exception: IllegalStateException) {
            Timber.tag(TAG).e("$exception")
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).v("onDestroy")
        viewModel.getHmsSdk()?.leave()
        viewModel.userSharedPreference.clearAll()
        super.onDestroy()
    }

    private fun setOnClickListener() {
        Timber.tag(TAG).v("setOnClickListener()")
        livestreamBinding?.let { livestreamBinding ->
            livestreamBinding.userCountLayout.setOnClickListener(this)
            livestreamBinding.orientationImageView.setOnClickListener(this)
        }
    }

    private fun setObserver() {
        Timber.tag(TAG).v("setObserver()")
        viewModel.icChatWindowHidden.observe(viewLifecycleOwner) {
            it?.let {
                livestreamBinding?.let {
                    onUserCountClick()
                    viewModel.closeChatWindow(null)
                }
            }
        }
        viewModel.unreadMessagesCount.observe(viewLifecycleOwner) { setUnReadMessageCount(it) }
        viewModel.isRoomEnded.observe(viewLifecycleOwner) { endRoom(it) }
        viewModel.roomStatus.observe(viewLifecycleOwner) { setRoomStatus(it) }
        viewModel.isJoined.observe(viewLifecycleOwner) { onRoomJoined(it) }
        viewModel.errorCode.observe(viewLifecycleOwner) { showErrors(it) }
        viewModel.resourceStatus.observe(viewLifecycleOwner) { setResourceStatus(it) }
        viewModel.peerList.observe(viewLifecycleOwner) { onPeerListReceived(it) }
        viewModel.networkQualityCode.observe(viewLifecycleOwner) { showNetworkStatus(it) }

        //        viewModel.peerCount.observe(viewLifecycleOwner) { showPeerCount(it) }
    }

    private fun onPeerListReceived(peerList: List<TrackPeerMap>) {
        livestreamBinding?.let { livestreamBinding ->
            Timber.tag(TAG).v("onPeerListReceived ${peerList.size}")
            if (peerList.isNotEmpty()) {
                livestreamBinding.statStreamTextView.visible(false)
            }
            val count = if (peerList.isEmpty()) {
                DEFAULT_SPAN_COUNT
            } else if (peerList.size <= MAX_SPAN_COUNT) {
                peerList.size
            } else {
                MAX_SPAN_COUNT
            }
            gridLayoutManager?.spanCount = count
            livestreamBinding.recyclerView.apply {
                layoutManager = gridLayoutManager
            }
            val runnable = Runnable {
                Timber.tag(TAG).v("Runnable")
                if (peerCount != peerList.size) {
                    Timber.tag(TAG).v("Runnable count has changed")
                    peerCount = peerList.size
                    peerAdapter?.notifyDataSetChanged()
                }
            }
            peerAdapter?.submitList(peerList, runnable)
        }
    }

    private fun onRoomJoined(isRoomJoined: Boolean?) {
        isRoomJoined?.let {
            livestreamBinding?.let { livestreamBinding ->
                livestreamBinding.userCountLayout.visible(it)
                livestreamBinding.statStreamTextView.visible(true)

                livestreamBinding.connectivityTextView.text = getString(R.string.workshop_joined)
                livestreamBinding.connectivityTextView.backgroundTintList =
                    resources.getColorStateList(R.color.lime_green, null)
                livestreamBinding.connectivityTextView.setTextColor(
                    resources.getColorStateList(R.color.white, null)
                )
                livestreamBinding.connectivityTextView.visible(true)
                livestreamBinding.chatFragmentContainerView.visible(true)

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    livestreamBinding.connectivityTextView.visible(false)
                }, CONNECTION_ONLINE_DELAY)

                viewModel.clearJoinFlag()
            }
        }
    }

    /**
     * if count is 3 digit then show 99+ and
     * width / height - 22dp
     * if count is 1 digit then
     * width / height - 20dp
     */
    private fun setUnReadMessageCount(count: Int) {
        Timber.tag(TAG).v("setUnReadMessageCount($count)")
        livestreamBinding?.let { livestreamBinding ->
            try {
                val dimen = if (count > UN_READ_MESSAGE_COUNT_THRESHOLD) {
                    R.dimen.size_22
                } else {
                    R.dimen.size_18
                }
                val size = requireContext().resources.getDimensionPixelSize(dimen)
                Timber.tag(TAG).v("setUnReadMessageCount $size")
                livestreamBinding.countTextView.width = size
                livestreamBinding.countTextView.height = size
            } catch (exception: IllegalStateException) {
                Timber.tag(TAG).e("setUnReadMessageCount $exception")
            }
            livestreamBinding.countTextView.text = "$count"
        }
    }

    private fun endRoom(isRoomEnded: Boolean) {
        Timber.tag(TAG).v("endRoom() $isRoomEnded")
        try {
            requireActivity().finish()
        } catch (exception: IllegalStateException) {
            Timber.tag(TAG).v("endRoom() $exception")
        }
    }

    override fun onClick(view: View) {
        livestreamBinding?.let { livestreamBinding ->
            when (view.id) {
                livestreamBinding.userCountLayout.id -> {
                    onUserCountClick()
                }
                livestreamBinding.orientationImageView.id -> {
                    onOrientationClick()
                }
            }
        }
    }

    private fun onUserCountClick() {
        viewModel.clearMessageReceivedCount()
        livestreamBinding?.let { livestreamBinding ->
            livestreamBinding.chatFragmentContainerView.visible(
                !livestreamBinding.chatFragmentContainerView.isVisible
            )
        }
    }

    private fun onOrientationClick() {
        livestreamBinding?.let { livestreamBinding ->
            when (requireActivity().requestedOrientation) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    toggleSystemBars(true)
                    livestreamBinding.orientationImageView.setImageResource(R.drawable.ic_fullscreen)
                }
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    toggleSystemBars(false)
                    livestreamBinding.orientationImageView.setImageResource(R.drawable.ic_normal)
                }
            }
        }
    }

    private fun showNetworkStatus(networkQualityCode: NetworkQualityCode) {
        livestreamBinding?.let { livestreamBinding ->
            livestreamBinding.networkStatusImageView.setImageResource(networkQualityCode.resourceId)
            livestreamBinding.networkStatusImageView.isVisible = true
        }
    }

    private fun setRoomStatus(roomStatus: RoomStatus) {
        Timber.tag(TAG).v("setRoomStatus($roomStatus)")
        livestreamBinding?.let { livestreamBinding ->
            if (!roomStatus.isSticky) {
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    livestreamBinding.connectivityTextView.visible(false)
                }, ROOM_STATUS_DELAY)
            }
            livestreamBinding.connectivityTextView.text = roomStatus.message
            livestreamBinding.connectivityTextView.backgroundTintList =
                resources.getColorStateList(R.color.raisin_black, null)
            livestreamBinding.connectivityTextView.setTextColor(
                resources.getColorStateList(R.color.white, null)
            )
            livestreamBinding.connectivityTextView.visible(true)
        }
    }

    private fun showErrors(apiErrorCodes: ApiErrorCodes?) {
        Timber.tag(TAG).e("showErrors $apiErrorCodes")
        livestreamBinding?.let { livestreamBinding ->
            apiErrorCodes?.let { error ->
                livestreamBinding.connectivityTextView.backgroundTintList =
                    resources.getColorStateList(R.color.raisin_black, null)
                livestreamBinding.connectivityTextView.setTextColor(
                    resources.getColorStateList(R.color.white, null)
                )
                livestreamBinding.connectivityTextView.text = error.description
                livestreamBinding.connectivityTextView.visible(true)
            }
        }
    }

    private fun setResourceStatus(resourceStatus: Resource<Any>?) {
        when (resourceStatus) {
            is Resource.Success -> {
                Timber.tag(TAG).v("setResourceStatus() Resource Success")
                dismissProgressDialog(true)
            }
            is Resource.Loading -> {
                Timber.tag(TAG).v("setResourceStatus() Resource Loading")
                resourceStatus.status?.let {
                    showProgressDialog(it, TAG, true)
                }
            }
            is Resource.Error -> {
                Timber.tag(TAG).v("setResourceStatus() Resource Error")
                dismissProgressDialog(true)
                handleApiError(resourceStatus)
                resourceStatus.errorBody?.let {
                    Timber.tag(TAG).v("$it")
                }
            }
            else -> {
                Timber.tag(TAG).v("setResourceStatus() No Resource")
            }
        }
    }

    private fun initPeerAdapter() {
        Timber.tag(TAG).v("initPeerAdapter()")
        livestreamBinding?.let { livestreamBinding ->
            try {
                peerAdapter = PeerScreenAdapter()
                gridLayoutManager = GridLayoutManager(
                    requireActivity(),
                    DEFAULT_SPAN_COUNT
                )
                livestreamBinding.recyclerView.apply {
                    layoutManager = gridLayoutManager
                    adapter = peerAdapter
                }
            } catch (exception: IllegalStateException) {
                Timber.tag(TAG).e("initPeerAdapter $exception")
            }
        }
    }

    private fun toggleSystemBars(isVisible: Boolean) {
        Timber.tag(TAG).v("toggleSystemBars()")
        try {
            val windowInsetsController =
                ViewCompat.getWindowInsetsController(requireActivity().window.decorView) ?: return
            if (isVisible) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        } catch (exception: IllegalStateException) {
            Timber.tag(TAG).e("toggleSystemBars $exception")
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG + "livestream").v("onResume()")
        livestreamBinding?.let { livestreamBinding ->
            if (livestreamHeight == null &&
                livestreamBinding.livestreamRoot.layoutParams.height > PLAYER_HEIGHT_DEFAULT
            ) {
                livestreamHeight = livestreamBinding.livestreamRoot.layoutParams.height
            }
        }
    }

    private fun extractData(userMetaData: String?): Boolean {
        Timber.tag(TAG + "livestream").v("extractData")
        var isValidUserData = false
        userMetaData?.let {
            userSharedPreference.saveStringParam(
                it,
                UserSharedPreference.USER_META_DATA
            )
            isValidUserData = true
        }
        return isValidUserData
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        livestreamBinding?.let { livestreamBinding ->
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Timber.tag(TAG).v("onConfigurationChanged Landscape")
                livestreamBinding.livestreamRoot.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Timber.tag(TAG).v("onConfigurationChanged Portrait")
                livestreamHeight?.let {
                    livestreamBinding.livestreamRoot.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        it
                    )
                }
            }
        }
    }
}
