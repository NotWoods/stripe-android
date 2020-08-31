package com.stripe.android.view

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.stripe.android.ApiRequest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.StripeApiRepository
import com.stripe.android.databinding.ActivityCheckoutBinding
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCheckoutBinding.inflate(layoutInflater)
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val args: Args? = intent.getParcelableExtra(EXTRA_ARGS)
        if (args != null) {
            Toast.makeText(this, "Found some args", Toast.LENGTH_LONG).show()
        }

        viewBinding.root.setOnClickListener {
            animateOut()
        }

        val bottomSheet: View = viewBinding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = STATE_HIDDEN

        supportFragmentManager
            .beginTransaction()
            .replace(viewBinding.fragmentContainer.id, CheckoutPaymentMethodListFragment())
            .commitAllowingStateLoss()

        withDelay(300) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == STATE_HIDDEN) {
                        finish()
                    }
                }
            })
        }
//        viewModel.transition.observe(this, Observer {
//            when(it) {
//                Transition.PUSH -> {
//                    supportFragmentManager.beginTransaction()
//                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
//                        .replace(R.id.fragment_container, AMCPushFragment())
//                        .addToBackStack(null)
//                        .commit()
//                }
//            }
//        })
    }

    private fun withDelay(delayMillis: Long, fn: () -> Unit) {
        lifecycleScope.launch {
            delay(delayMillis)
            withContext(Dispatchers.Main) {
                fn()
            }
        }
    }

    private fun animateOut() {
        bottomSheetBehavior.state = STATE_HIDDEN
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            animateOut()
        }
    }

    override fun finish() {
        super.finish()
        // TOOD set result
        overridePendingTransition(0, 0)
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val ephemeralKey: String,
        val customer: String
    ) : Parcelable

    internal class ViewModel(application: Application) : AndroidViewModel(application) {
        val config = PaymentConfiguration.getInstance(application)

        val publishableKey = config.publishableKey
        private val stripeRepository = StripeApiRepository(application, publishableKey, Stripe.appInfo)

        fun getPaymentMethods(customerId: String, privateKey: String): LiveData<List<PaymentMethod>> = liveData(Dispatchers.IO) {
            val result = stripeRepository.getPaymentMethods(
                ListPaymentMethodsParams(
                    customerId = customerId,
                    paymentMethodType = PaymentMethod.Type.Card
                ),
                publishableKey,
                setOf(),
                ApiRequest.Options(privateKey, config.stripeAccountId)
            )
            emit(result)
        }
    }

    companion object {
        internal const val EXTRA_ARGS = "checkout_activity_args"

        fun start(activity: Activity, clientSecret: String, ephemeralKey: String, customer: String) {
            activity.startActivity(Intent(activity, CheckoutActivity::class.java)
                .putExtra(EXTRA_ARGS, Args(clientSecret, ephemeralKey, customer))
                .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }
}