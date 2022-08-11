package com.supercoolapps.memorytest

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.supercoolapps.models.BoardSize
import com.supercoolapps.utils.BitMapScaler
import com.supercoolapps.utils.EXTRA_BOARD_SIZE
import com.supercoolapps.utils.isPermissionGranted
import com.supercoolapps.utils.requestPermission
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTOS_REQUEST_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 656
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button

    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById<EditText>(R.id.etGameName)
        btnSave = findViewById<Button>(R.id.btnSave)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0 / ${numImagesRequired})"

        btnSave.setOnClickListener{
            saveDataToFireBase()
        }
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shoulEnableSaveButton()
            }
        })

        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos()
                }else{
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun saveDataToFireBase() {
        val customGameName = etGameName.text.toString()
        var didEncounterError = false
        val uploadedImageUrl = mutableListOf<String>()
       Log.i(TAG, "SaveDataToFirebase!")
        for((index, photouri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photouri)
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
            }.addOnCompleteListener{ downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful){
                        Log.i(TAG, "Exception with firebase storage ", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload images", Toast.LENGTH_LONG).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError){
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrl += downloadUrl
                    Log.i(TAG, "Finished upload $photouri!, num uploaded ${uploadedImageUrl.size}")
                    if(uploadedImageUrl.size == chosenImageUris.size){
                        handleAllImagesUploaded(customGameName, uploadedImageUrl)
                    }
                }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
//            TODO: upload this into firestore...
    }


    private fun getImageByteArray(photouri: Uri): ByteArray {
        val originalBitMap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(contentResolver, photouri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(contentResolver, photouri)
        }
        Log.i(TAG, "Original width ${originalBitMap.width} and height is : ${originalBitMap.height}")
        val scaledBitMap = BitMapScaler.scaleToFitHeight(originalBitMap, 250)
        Log.i(TAG, "Original width ${scaledBitMap.width} and height is : ${scaledBitMap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitMap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }else{
                Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTOS_REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.w(TAG, "Did not get data back from the launched activity, user cancelled flow!")
            return
        }
        val selectedUri: Uri? = data.data
        val clipData = data.clipData

        if(clipData != null){
            Log.i(TAG, "clipData images ${clipData.itemCount} : ${clipData}")
            for(i in 0 until clipData.itemCount){
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < numImagesRequired){
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri != null){
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris += selectedUri
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics ( $chosenImageUris.size / $numImagesRequired )"
        btnSave.isEnabled = shoulEnableSaveButton()
    }

    private fun shoulEnableSaveButton(): Boolean {
        if(chosenImageUris.size != numImagesRequired){
            return  false
        }
        if(etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type  = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTOS_REQUEST_CODE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}