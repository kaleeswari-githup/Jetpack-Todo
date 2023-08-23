package com.example.Pages

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dothings.R
import com.example.dothings.Screen
import com.example.dothings.interDisplayFamily
import com.example.ui.theme.SurfaceGray
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


lateinit var googleSignInClient: GoogleSignInClient
lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
@Composable
fun SignInScreen(navController: NavController){
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Check if the user is already signed
    LaunchedEffect(Unit ){
        if (currentUser != null) {
            navController.navigate(Screen.Home.route)
        }
    }



    val context = LocalContext.current
// Initialize GoogleSignInOptions
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("664988258330-3ic7tbaom8eeruprcj0lktomos8bnrdo.apps.googleusercontent.com")
        .requestEmail()
        .build()

// Create GoogleSignInClient
    googleSignInClient = GoogleSignIn.getClient(LocalContext.current, gso)

// Create ActivityResultLauncher
    activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                // Get Google Sign-In account
                val account = task.getResult(ApiException::class.java)
                // Use the account to sign in to Firebase
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            // Sign-in success
                            navController.navigate(Screen.Home.route)
                            Toast.makeText(context,  "success", Toast.LENGTH_SHORT).show()
                            val user = auth.currentUser

                            // Do something with the user
                        } else {
                            // Sign-in failed
                            // Handle the failure
                        }
                    }
                    .addOnFailureListener{exception ->
                        Log.e("SignInScreen", "Sign-in failed: ${exception.message}", exception)
                    }
            } catch (e: ApiException) {
                // Handle sign-in error
                Toast.makeText(context,  e.localizedMessage, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = SurfaceGray),

    ){
        var isLoading by remember { mutableStateOf(false) }
        Image(painter = painterResource(id = R.drawable.grid_lines), contentDescription = null)
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center){
            Image(painter = painterResource(id = R.drawable.shadowcenter), contentDescription = null,
                modifier = Modifier
                    .graphicsLayer(alpha = 0.06f)
                    .blur(radius = 84.dp)
                    .align(Alignment.Center))
        }
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(id = R.drawable.black_tick_ball), contentDescription = "")
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp, end = 42.dp,top = 172.dp)
                    .height(72.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(64.dp))
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        val signInIntent = googleSignInClient.signInIntent
                        activityResultLauncher.launch(signInIntent)

                    },
                    contentAlignment = Alignment.Center
                ){
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ){
                        Image(painter = painterResource(id = R.drawable.google_icon), contentDescription = "")
                        Text(
                            text = "Continue with Google",
                            fontFamily = interDisplayFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

    }
}

@Preview
@Composable
fun signinPreview(){
    SignInScreen(navController = rememberNavController())
}

