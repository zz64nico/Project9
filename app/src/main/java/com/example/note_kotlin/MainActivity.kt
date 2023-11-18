package com.example.note_kotlin

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.note_kotlin.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import java.util.*


/**
 * Activity to upload and download photos from Firebase Storage.
 *
 * See [MyUploadService] for upload example.
 * See [MyDownloadService] for download example.
 */
class MainActivity : AppCompatActivity(), View.OnClickListener,INoteListener {

    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var auth: FirebaseAuth

    private var downloadUrl: Uri? = null
    private var fileUri: Uri? = null

    private lateinit var binding: ActivityMainBinding

    private lateinit var cameraIntent: ActivityResultLauncher<Array<String>>
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                this,
                "Can't post notifications without POST_NOTIFICATIONS permission",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    lateinit var noteAdapter: NoteAdapter
    lateinit var stringList:MutableList<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        stringList = ArrayList()
        noteAdapter = NoteAdapter(this,this);
        binding.rlImg.layoutManager = GridLayoutManager(this,3);
        binding.rlImg.addItemDecoration(MyItemDecoration(10))
        binding.rlImg.adapter = noteAdapter;
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // Click listeners
        with(binding) {
            buttonCamera.setOnClickListener(this@MainActivity)
            buttonSignIn.setOnClickListener(this@MainActivity)
        }


        // Local broadcast receiver
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "onReceive:$intent")
                hideProgressBar()

                when (intent.action) {
                    MyDownloadService.DOWNLOAD_COMPLETED -> {
                        // Get number of bytes downloaded
                        val numBytes = intent.getLongExtra(MyDownloadService.EXTRA_BYTES_DOWNLOADED, 0)

                        // Alert success
                        showMessageDialog(
                            getString(R.string.success),
                            String.format(
                                Locale.getDefault(),
                                "%d bytes downloaded from %s",
                                numBytes,
                                intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH),
                            ),
                        )
                    }
                    MyDownloadService.DOWNLOAD_ERROR ->
                        // Alert failure
                        showMessageDialog(
                            "Error",
                            String.format(
                                Locale.getDefault(),
                                "Failed to download from %s",
                                intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH),
                            ),
                        )
                    MyUploadService.UPLOAD_COMPLETED, MyUploadService.UPLOAD_ERROR -> onUploadResultIntent(intent)
                }
            }
        }

        // Restore instance state
        savedInstanceState?.let {
            fileUri = it.getParcelable(KEY_FILE_URI)
            downloadUrl = it.getParcelable(KEY_DOWNLOAD_URL)
        }
        onNewIntent(intent)

        askNotificationPermission()


        val listRef: StorageReference =  Firebase.storage.reference.child("photos")

        listRef.listAll()
            .addOnSuccessListener { listResult ->
                for (item in listResult.items) {
                    // All the items under listRef.
                    item.downloadUrl.addOnSuccessListener {downloadUri->
                        stringList.add(downloadUri.toString())
                        App.stringList = stringList
                        noteAdapter.setNewData(stringList)
                        noteAdapter.notifyDataSetChanged()
                    }
                }

            }
            .addOnFailureListener {
                // Uh-oh, an error occurred!
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (App.commonUri!=null){
            uploadFromUri(App.commonUri!!)
        }
        val listRef: StorageReference =  Firebase.storage.reference.child("photos")
        stringList.clear()
        listRef.listAll()
            .addOnSuccessListener { listResult ->
                for (item in listResult.items) {
                    // All the items under listRef.
                    item.downloadUrl.addOnSuccessListener {downloadUri->
                        stringList.add(downloadUri.toString())
                        App.stringList = stringList
                        noteAdapter.setNewData(stringList)
                        noteAdapter.notifyDataSetChanged()
                    }
                }

            }
            .addOnFailureListener {
                // Uh-oh, an error occurred!
            }
    }
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(MyUploadService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent)
        }
    }

    public override fun onStart() {
        super.onStart()
        updateUI(auth.currentUser)

        // Register receiver for uploads and downloads
        val manager = LocalBroadcastManager.getInstance(this)
        manager.registerReceiver(broadcastReceiver, MyDownloadService.intentFilter)
        manager.registerReceiver(broadcastReceiver, MyUploadService.intentFilter)
    }

    public override fun onStop() {
        super.onStop()

        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    public override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putParcelable(KEY_FILE_URI, fileUri)
        out.putParcelable(KEY_DOWNLOAD_URL, downloadUrl)
    }

    private fun uploadFromUri(uploadUri: Uri) {
        Log.d(TAG, "uploadFromUri:src: $uploadUri")

        // Save the File URI
        fileUri = uploadUri

        // Clear the last download, if any
        updateUI(auth.currentUser)
        downloadUrl = null

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Activity is killed or put in the background
        startService(
            Intent(this, MyUploadService::class.java)
                .putExtra(MyUploadService.EXTRA_FILE_URI, uploadUri)
                .setAction(MyUploadService.ACTION_UPLOAD),
        )

        // Show loading spinner
        showProgressBar(getString(R.string.progress_uploading))
    }



    private fun launchCamera() {
        Log.d(TAG, "launchCamera")

        // Pick an image from storage
//        cameraIntent.launch(arrayOf("image/*"))
        startActivityForResult(Intent(this@MainActivity,IdentificationPhoto::class.java),101)
    }



    private fun signInAnonymously() {
        // Sign in anonymously. Authentication is required to read or write from Firebase Storage.
        showProgressBar(getString(R.string.progress_auth))
        auth.signInAnonymously()
            .addOnSuccessListener(this) { authResult ->
                Log.d(TAG, "signInAnonymously:SUCCESS")
                hideProgressBar()
                updateUI(authResult.user)
            }
            .addOnFailureListener(this) { exception ->
                Log.e(TAG, "signInAnonymously:FAILURE", exception)
                hideProgressBar()
                updateUI(null)
            }
    }

    private fun onUploadResultIntent(intent: Intent) {
        // Got a new intent from MyUploadService with a success or failure
        downloadUrl = intent.getParcelableExtra(MyUploadService.EXTRA_DOWNLOAD_URL)
        fileUri = intent.getParcelableExtra(MyUploadService.EXTRA_FILE_URI)

        updateUI(auth.currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        with(binding) {
            // Signed in or Signed out
            if (user != null) {
                layoutSignin.visibility = View.GONE
                layoutStorage.visibility = View.VISIBLE
            } else {
                layoutSignin.visibility = View.VISIBLE
                layoutStorage.visibility = View.GONE
            }

        }
    }

    private fun showMessageDialog(title: String, message: String) {
        val ad = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .create()
        ad.show()
    }

    private fun showProgressBar(progressCaption: String) {
        with(binding) {
            caption.text = progressCaption
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        with(binding) {
            caption.text = ""
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Your app can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonCamera -> launchCamera()
            R.id.buttonSignIn -> signInAnonymously()
        }
    }

    companion object {

        private const val TAG = "Storage#MainActivity"

        private const val KEY_FILE_URI = "key_file_uri"
        private const val KEY_DOWNLOAD_URL = "key_download_url"
    }

    override fun onNoteClick(url: String) {
        var intent = Intent(this@MainActivity,ImageActivity::class.java);
        intent.putExtra("url",url);
        startActivity(intent)

    }
}
