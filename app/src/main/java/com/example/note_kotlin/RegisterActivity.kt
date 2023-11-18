package com.example.note_kotlin

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.note_kotlin.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity:AppCompatActivity() {
    lateinit var binding:ActivityRegisterBinding ;
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_register);
        auth = FirebaseAuth.getInstance()
        binding.submit.setOnClickListener {
            val name = binding.account.text.toString();
            val password = binding.password.text.toString();
            val pwd = binding.passwordAgain.text.toString();
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password) || TextUtils.isEmpty(pwd)){
                Toast.makeText(applicationContext,"input name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!password.equals(pwd)){
                Toast.makeText(applicationContext,"error",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            kotlin.run {

            }
            auth.createUserWithEmailAndPassword(name, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user = auth.currentUser
                        Toast.makeText(applicationContext,"register success",Toast.LENGTH_SHORT).show()
                        Log.d("处处出",user.toString());
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}