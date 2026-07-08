package com.borrowhub.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.borrowhub.app.data.Borrow
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("BorrowHubPrefs", Context.MODE_PRIVATE)
        val isH1Active = prefs.getBoolean("PREF_H1_REMINDER", true)
        val isHariHActive = prefs.getBoolean("PREF_HARI_H_REMINDER", true)

        // Jika kedua pengaturan dimatikan di Profil, hentikan tugas
        if (!isH1Active && !isHariHActive) return Result.success()

        return try {
            val db = FirebaseFirestore.getInstance()

            // Tasks.await() memaksa proses menunggu data Firestore selesai ditarik di background thread
            val snapshot = Tasks.await(db.collection("borrows")
                .whereIn("status", listOf("Active", "Overdue"))
                .get())

            val today = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

            for (doc in snapshot.documents) {
                val borrow = doc.toObject(Borrow::class.java) ?: continue
                val endDate = borrow.endDate?.toDate() ?: continue
                val endCal = Calendar.getInstance().apply { time = endDate }

                // Pengecekan Hari-H
                if (isHariHActive && isSameDay(today, endCal)) {
                    showNotification(
                        id = borrow.id.hashCode(),
                        title = "Peringatan Jatuh Tempo!",
                        message = "Barang ${borrow.itemName} yang dipinjam ${borrow.borrowerName} harus dikembalikan HARI INI."
                    )
                }
                // Pengecekan H-1
                else if (isH1Active && isSameDay(tomorrow, endCal)) {
                    showNotification(
                        id = borrow.id.hashCode(),
                        title = "H-1 Jatuh Tempo",
                        message = "Barang ${borrow.itemName} yang dipinjam ${borrow.borrowerName} jatuh tempo BESOK."
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Gagal menjalankan worker: ${e.message}")
            Result.retry()
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showNotification(id: Int, title: String, message: String) {
        val channelId = "borrowhub_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Membuat Channel Notifikasi wajib untuk Android 8.0 (Oreo) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pengingat Peminjaman", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Ikon bawaan Android
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }
}