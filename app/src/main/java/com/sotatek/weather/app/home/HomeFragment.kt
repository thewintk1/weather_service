package com.sotatek.weather.app.home

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.sotatek.weather.R
import com.sotatek.weather.base.BaseFragment
import com.sotatek.weather.data.remote.errors.ViewError
import com.sotatek.weather.databinding.FragmentHomeBinding
import com.sotatek.weather.extension.getIconUrl
import com.sotatek.weather.extension.toFahrenheit
import com.sotatek.weather.util.channelID
import com.sotatek.weather.util.messageExtra
import com.sotatek.weather.util.notificationID
import com.sotatek.weather.util.titleExtra
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber.d
import java.util.*

/**
 * Created by khiemnt
 */

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(FragmentHomeBinding::inflate) {

    override val viewModel: HomeViewModel by viewModels()

    override fun initializeViews() {
        createNotificationChannel()
        if (viewModel.city.isNotBlank()) {
            viewModel.getWeather()
        }
        binding.apply {
            btnSearch.setOnClickListener {
                scheduleNotification()
                if (edtCity.text.toString().isNotEmpty()) {
                    viewModel.city = edtCity.text.toString()
                    viewModel.getWeather()
                } else {
                    showError(getString(R.string.please_enter_city))
                }
            }
            textViewForecast.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionOpenForecastFragment(viewModel.city))
            }
            weatherItem.apply {
                viewModel.weatherResultDto
                    .onEach { weatherResultDto ->
                        binding.llWeatherContent.isVisible = true
                        val weather = weatherResultDto.weather[0]
                        val icon = weather.icon
                        Glide.with(requireActivity())
                            .load(getIconUrl(icon))
                            .into(imgIcon)

                        val weatherBasicDto = weatherResultDto.weatherBasicDto
                        viewModel.temp = weatherBasicDto.temp.toInt()
                        showTemperature(viewModel.showTempC)
                        textHumidity.text = String.format(getString(R.string.humidity), "${weatherBasicDto.humidity.toInt()} %")
                        textDescription.text = String.format(getString(R.string.description_weather), weather.main, weather.description)
                        textCountry.text = viewModel.city

                        textHumidity.isVisible = true
                        textDescription.isVisible = true
                        textViewForecast.isVisible = true
                        textCountry.isVisible = true
                    }
                    .launchIn(lifecycleScope)

                textTempC.setOnClickListener {
                    if (viewModel.showTempC) return@setOnClickListener
                    viewModel.showTempC = true
                    showTemperature(viewModel.showTempC)
                }
                textTempF.setOnClickListener {
                    if (!viewModel.showTempC) return@setOnClickListener
                    viewModel.showTempC = false
                    showTemperature(viewModel.showTempC)
                }
            }
        }
    }
    private fun createNotificationChannel()
    {
        val name = "Notif Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = context!!.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    private fun scheduleNotification()
    {
        val intent = Intent(requireActivity(), Notification::class.java)
        val title = "binding.titleET.text.toString()"
        val message = "binding.messageET.text.toString()"
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val pendingIntent = PendingIntent.getBroadcast(
            requireActivity(),
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val time = Calendar.getInstance()
//        time.add(Calendar.SECOND, 5)

        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time.timeInMillis,
            pendingIntent
        )
    }
    private fun showTemperature(showTempC: Boolean) {
        binding.weatherItem.apply {
            textTemperature.isVisible = true
            textTempC.isVisible = true
            textTempF.isVisible = true

            val activeTempC = if (showTempC) R.color.black else R.color.gray
            val activeTempF = if (!showTempC) R.color.black else R.color.gray
            textTempC.setTextColor(ContextCompat.getColor(requireContext(), activeTempC))
            textTempF.setTextColor(ContextCompat.getColor(requireContext(), activeTempF))
            textTemperature.text = when (showTempC) {
                true -> viewModel.temp.toString()
                else -> viewModel.temp.toFahrenheit().toString()
            }
        }
    }

    override fun handleError(viewError: ViewError) {
        super.handleError(viewError)
        binding.llWeatherContent.isVisible = false
    }
}