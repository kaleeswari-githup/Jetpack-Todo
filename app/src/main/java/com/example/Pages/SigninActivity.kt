package com.example.Pages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.dothings.R
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.Pages.ui.theme.DoThingsTheme
import com.example.dothings.MainActivity
import com.example.ui.theme.AppJetpackComposeTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay

class SigninActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppJetpackComposeTheme {
                // A surface container using the 'background' color from the theme
                SignInScreen()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val auth: FirebaseAuth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if(user != null){
            val intent = Intent(this@SigninActivity,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

@Composable
fun SignInScreen(){
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("664988258330-3ic7tbaom8eeruprcj0lktomos8bnrdo.apps.googleusercontent.com")
        .requestEmail()
        .build()
    googleSignInClient = GoogleSignIn.getClient(LocalContext.current, gso)
    activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                        } else {
                        }
                    }
                    .addOnFailureListener{exception ->
                        Log.e("SignInScreen", "Sign-in failed: ${exception.message}", exception)
                    }
            } catch (e: ApiException) {
                Toast.makeText(context,  e.localizedMessage, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)
    ){
        var visible by remember {
            mutableStateOf(false)
        }
        LaunchedEffect(visible) {
            visible = true
        }
        var googleVisible by remember {
            mutableStateOf(false)
        }
        LaunchedEffect(googleVisible) {
            delay(100)
            googleVisible = true
        }
        ThemedGridImage()
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally) {
                val offsetY by animateDpAsState(
                    targetValue = if (visible) 0.dp else 32.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    ),
                )
                val scale by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = keyframes {
                        durationMillis = 500
                    }
                )
                val googleOffsetY by animateDpAsState(
                    targetValue = if (googleVisible) 0.dp else 32.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    )
                )
                val googleScale by animateFloatAsState(
                    targetValue = if (googleVisible) 1f else 0f,
                    animationSpec = keyframes {
                        durationMillis = 500
                    }
                )
                ThemedImage(modifier = Modifier
                    .padding(top = 120.dp)
                    .offset(y = offsetY)
                    .alpha(scale))
                Box(modifier = Modifier
                    .wrapContentWidth()
                    .offset(y = googleOffsetY)
                    .alpha(googleScale)
                    .padding(bottom = 152.dp)
                    .height(72.dp)
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = RoundedCornerShape(64.dp)
                    )
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        val signInIntent = googleSignInClient.signInIntent
                        activityResultLauncher.launch(signInIntent)
                        Vibration(context)

                    },
                    contentAlignment = Alignment.Center
                ){
                    Row(modifier = Modifier
                        .wrapContentWidth()
                        .padding(start = 48.dp, end = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ){
                        Image(painter = painterResource(id = R.drawable.google_icon), contentDescription = "")
                        Spacer(modifier = Modifier.padding(start = 16.dp))
                        ButtonTextWhiteTheme(text = ("Continue with Google").uppercase())
                    }
                }
            }
        }
    }
}

@Composable
fun ThemedImage(modifier:Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    val imageRes = if (isDarkTheme) {
        R.drawable.dark_theme_ball
    } else {
        R.drawable.black_tick_ball
    }
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier
    )
}