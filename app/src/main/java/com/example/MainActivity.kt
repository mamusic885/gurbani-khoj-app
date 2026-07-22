package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.db.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.snapshotFlow
import android.content.Intent

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val database = AppDatabase.getDatabase(this)
    val repository = BookmarkRepository(database.bookmarkDao())
    val viewModelFactory = BookmarksViewModelFactory(repository)
    val viewModel = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[BookmarksViewModel::class.java]
    val settingsManager = SettingsManager(applicationContext)

    setContent {
      val settingsState by settingsManager.settings.collectAsStateWithLifecycle()
      val isDark = when (settingsState.themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
      }

      val view = androidx.compose.ui.platform.LocalView.current
      LaunchedEffect(settingsState.keepScreenOn) {
        view.keepScreenOn = settingsState.keepScreenOn
      }

      MyApplicationTheme(darkTheme = isDark) {
        MainApp(viewModel, settingsManager)
      }
    }
  }
}

// Gurbani Offline Data Models
data class Bani(
  val fileName: String,
  val title: String,
  val verses: List<Verse>
)

data class Verse(
  val id: Int,
  val index: Int,
  val line: String,
  val pauseType: String? = null,
  val bookmarked: Boolean = false,
  val translation: String = ""
)

data class SearchResult(
  val baniName: String,
  val fileName: String,
  val verse: Verse
)

fun getBaniFileName(title: String): String {
  return when (title) {
    "ਜਪੁਜੀ ਸਾਹਿਬ" -> "japji_sahib.json"
    "ਜਾਪੁ ਸਾਹਿਬ" -> "jaap_sahib.json"
    "ਤ੍ਵ ਪ੍ਰਸਾਦਿ ਸਵੱਯੇ" -> "tav_prasad_savaiye.json"
    "ਚੌਪਈ ਸਾਹਿਬ" -> "chaupai_sahib.json"
    "ਅਨੰਦ ਸਾਹਿਬ" -> "anand_sahib.json"
    "ਰਹਿਰਾਸ ਸਾਹਿਬ" -> "rehras_sahib.json"
    "ਅਰਦਾਸ" -> "ardas.json"
    "ਕੀਰਤਨ ਸੋਹਿਲਾ" -> "kirtan_sohila.json"
    "ਆਰਤੀ" -> "aarti.json"
    "ਆਸਾ ਦੀ ਵਾਰ" -> "asa_di_vaar.json"
    "ਸ੍ਰੀ ਸੁਖਮਨੀ ਸਾਹਿਬ" -> "sri_sukhmani_sahib.json"
    else -> ""
  }
}

fun loadBaniFromAsset(context: android.content.Context, fileName: String): Bani {
  return try {
    val jsonString = context.assets.open("bani/$fileName").bufferedReader().use { it.readText() }
    val jsonObject = org.json.JSONObject(jsonString)
    val title = jsonObject.getString("title")
    val versesArray = jsonObject.getJSONArray("verses")
    val verses = mutableListOf<Verse>()
    for (i in 0 until versesArray.length()) {
      val verseObj = versesArray.getJSONObject(i)
      val id = verseObj.optInt("id", i)
      val line = verseObj.getString("line")
      val pauseType = if (verseObj.has("pauseType")) verseObj.getString("pauseType") else null
      val bookmarked = verseObj.optBoolean("bookmarked", false)
      val translation = verseObj.optString("translation", "")
      verses.add(Verse(id = id, index = i, line = line, pauseType = pauseType, bookmarked = bookmarked, translation = translation))
    }
    Bani(fileName = fileName, title = title, verses = verses)
  } catch (e: Exception) {
    e.printStackTrace()
    val fallbackTitle = when (fileName) {
      "japji_sahib.json" -> "ਜਪੁਜੀ ਸਾਹਿਬ"
      "jaap_sahib.json" -> "ਜਾਪੁ ਸਾਹਿਬ"
      "tav_prasad_savaiye.json" -> "ਤ੍ਵ ਪ੍ਰਸਾਦਿ ਸਵੱਯੇ"
      "chaupai_sahib.json" -> "ਚੌਪਈ ਸਾਹਿਬ"
      "anand_sahib.json" -> "ਅਨੰਦ ਸਾਹਿਬ"
      "rehras_sahib.json" -> "ਰਹਿਰਾਸ ਸਾਹਿਬ"
      "ardas.json" -> "ਅਰਦਾਸ"
      "kirtan_sohila.json" -> "ਕੀਰਤਨ ਸੋਹਿਲਾ"
      "aarti.json" -> "ਆਰਤੀ"
      "asa_di_vaar.json" -> "ਆਸਾ ਦੀ ਵਾਰ"
      "sri_sukhmani_sahib.json" -> "ਸ੍ਰੀ ਸੁਖਮਨੀ ਸਾਹਿਬ"
      else -> fileName.replace(".json", "").replace("_", " ").capitalize()
    }
    Bani(fileName = fileName, title = fallbackTitle, verses = emptyList())
  }
}

sealed class Screen {
  object Welcome : Screen()
  object Home : Screen()
  object Nitnem : Screen()
  object Search : Screen()
  object Bookmarks : Screen()
  object Settings : Screen()
  object About : Screen()
  data class BaniDetail(val name: String, val highlightIndex: Int? = null) : Screen()
}

@Composable
fun MainApp(viewModel: BookmarksViewModel, settingsManager: SettingsManager) {
  val settingsState by settingsManager.settings.collectAsStateWithLifecycle()
  var navigationStack by remember { mutableStateOf<List<Screen>>(emptyList()) }

  LaunchedEffect(settingsState.isFirstLaunchDone) {
    if (navigationStack.isEmpty()) {
      navigationStack = if (settingsState.isFirstLaunchDone) {
        listOf(Screen.Home)
      } else {
        listOf(Screen.Welcome)
      }
    }
  }

  if (navigationStack.isEmpty()) return

  val currentScreen = navigationStack.last()

  androidx.activity.compose.BackHandler(enabled = navigationStack.size > 1) {
    navigationStack = navigationStack.dropLast(1)
  }

  when (currentScreen) {
    is Screen.Welcome -> {
      WelcomeScreen(
        onGetStarted = {
          settingsManager.setFirstLaunchDone(true)
          navigationStack = listOf(Screen.Home)
        }
      )
    }
    is Screen.Home -> {
      HomeScreen(
        onNavigateToNitnem = {
          navigationStack = navigationStack + Screen.Nitnem
        },
        onNavigateToSearch = {
          navigationStack = navigationStack + Screen.Search
        },
        onNavigateToBookmarks = {
          navigationStack = navigationStack + Screen.Bookmarks
        },
        onNavigateToSettings = {
          navigationStack = navigationStack + Screen.Settings
        },
        onNavigateToAbout = {
          navigationStack = navigationStack + Screen.About
        }
      )
    }
    is Screen.Nitnem -> {
      NitnemScreen(
        onBack = {
          if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
          }
        },
        onNavigateToBani = { baniName ->
          navigationStack = navigationStack + Screen.BaniDetail(name = baniName)
        }
      )
    }
    is Screen.Search -> {
      SearchScreen(
        onBack = {
          if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
          }
        },
        onNavigateToBani = { baniName, lineIndex ->
          navigationStack = navigationStack + Screen.BaniDetail(name = baniName, highlightIndex = lineIndex)
        }
      )
    }
    is Screen.Bookmarks -> {
      BookmarksScreen(
        viewModel = viewModel,
        onBack = {
          if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
          }
        },
        onNavigateToBani = { baniName, lineIndex ->
          navigationStack = navigationStack + Screen.BaniDetail(name = baniName, highlightIndex = lineIndex)
        }
      )
    }
    is Screen.BaniDetail -> {
      BaniDetailScreen(
        baniName = currentScreen.name,
        highlightIndex = currentScreen.highlightIndex,
        viewModel = viewModel,
        settingsManager = settingsManager,
        onBack = {
          if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
          }
        }
      )
    }
    is Screen.Settings -> {
      SettingsScreen(
        settingsManager = settingsManager,
        onBack = {
          if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
          }
        }
      )
    }
    is Screen.About -> {
      AboutScreen(
        onBack = {
          if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
          }
        }
      )
    }
  }
}

@Composable
fun HomeScreen(
  onNavigateToNitnem: () -> Unit,
  onNavigateToSearch: () -> Unit,
  onNavigateToBookmarks: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToAbout: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var visible by remember { mutableStateOf(false) }

  // Entrance animation trigger
  LaunchedEffect(Unit) {
    visible = true
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("home_screen_scaffold"),
    containerColor = Color.White,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(Color.White)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Upper section: Header, Title, and Accent Divider
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(top = 24.dp)
        ) {
          // Animated Ik Onkar symbol
          AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = spring()) + slideInVertically(
              initialOffsetY = { -40 },
              animationSpec = spring()
            )
          ) {
            Text(
              text = "ੴ",
              style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                color = SaffronPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              ),
              modifier = Modifier
                .padding(bottom = 2.dp)
                .testTag("ek_onkar_logo")
            )
          }

          // Animated App Title & Subtitle
          AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = spring()) + slideInVertically(
              initialOffsetY = { 40 },
              animationSpec = spring()
            )
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "ਗੁਰਬਾਣੀ ਖੋਜ",
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 28.sp,
                  color = TextMedium,
                  letterSpacing = (-0.5).sp,
                  textAlign = TextAlign.Center
                ),
                modifier = Modifier.testTag("app_title")
              )
              
              Spacer(modifier = Modifier.height(6.dp))
              Box(
                modifier = Modifier
                  .width(48.dp)
                  .height(4.dp)
                  .background(
                    color = SaffronPrimary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                  )
              )

              Spacer(modifier = Modifier.height(6.dp))
              
              Text(
                text = "ਸ੍ਰੀ ਗੁਰੂ ਗ੍ਰੰਥ ਸਾਹਿਬ ਜੀ ਅਤੇ ਨਿਤਨੇਮ ਪਾਠ",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextGray,
                  fontSize = 13.sp,
                  textAlign = TextAlign.Center
                ),
                modifier = Modifier.testTag("app_subtitle")
              )
            }
          }
        }

        // Middle section containing Polish Styled Menu Cards
        AnimatedVisibility(
          visible = visible,
          enter = fadeIn(animationSpec = spring(stiffness = 100f)) + slideInVertically(
            initialOffsetY = { 80 },
            animationSpec = spring(stiffness = 100f)
          ),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            val menuItems = listOf(
              MenuData(
                emoji = "🔍",
                title = "ਗੁਰਬਾਣੀ ਖੋਜ",
                testTag = "search_gurbani_button",
                toastMessage = "ਗੁਰਬਾਣੀ ਖੋਜ",
                buttonType = ButtonType.PRIMARY
              ),
              MenuData(
                emoji = "📖",
                title = "ਨਿਤਨੇਮ ਸਾਹਿਬ",
                testTag = "nitnem_sahib_button",
                toastMessage = "ਨਿਤਨੇਮ ਸਾਹਿਬ",
                buttonType = ButtonType.SECONDARY
              ),
              MenuData(
                emoji = "⭐",
                title = "ਬੁੱਕਮਾਰਕ",
                testTag = "bookmarks_button",
                toastMessage = "ਬੁੱਕਮਾਰਕ",
                buttonType = ButtonType.SECONDARY
              ),
              MenuData(
                emoji = "⚙️",
                title = "ਸੈਟਿੰਗਾਂ",
                testTag = "settings_button",
                toastMessage = "ਸੈਟਿੰਗਾਂ",
                buttonType = ButtonType.DEFAULT
              ),
              MenuData(
                emoji = "ℹ️",
                title = "ਐਪ ਬਾਰੇ",
                testTag = "about_button",
                toastMessage = "ਐਪ ਬਾਰੇ",
                buttonType = ButtonType.DEFAULT
              )
            )

            menuItems.forEach { item ->
              HomeMenuItem(
                emoji = item.emoji,
                title = item.title,
                testTag = item.testTag,
                buttonType = item.buttonType,
                onClick = {
                  if (item.testTag == "nitnem_sahib_button") {
                    onNavigateToNitnem()
                  } else if (item.testTag == "search_gurbani_button") {
                    onNavigateToSearch()
                  } else if (item.testTag == "bookmarks_button") {
                    onNavigateToBookmarks()
                  } else if (item.testTag == "settings_button") {
                    onNavigateToSettings()
                  } else if (item.testTag == "about_button") {
                    onNavigateToAbout()
                  } else {
                    scope.launch {
                      snackbarHostState.currentSnackbarData?.dismiss()
                      snackbarHostState.showSnackbar(item.toastMessage)
                    }
                  }
                },
                modifier = Modifier.padding(vertical = 6.dp)
              )
            }
          }
        }

        // Footer Section: Material 3 Navigation Gesture Bar Placeholder / Accent
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        ) {
          Text(
            text = "ੴ ਸਤਿਗੁਰ ਪ੍ਰਸਾਦਿ",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SaffronPrimary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
              letterSpacing = 1.5.sp,
              textAlign = TextAlign.Center
            ),
            modifier = Modifier
              .padding(bottom = 16.dp)
              .testTag("footer_attribution")
          )
          
          // Slate navigation gesture indicator bar
          Box(
            modifier = Modifier
              .width(128.dp)
              .height(6.dp)
              .background(
                color = Slate200,
                shape = RoundedCornerShape(3.dp)
              )
          )
        }
      }
    }
  }
}

enum class ButtonType {
  PRIMARY, SECONDARY, DEFAULT
}

data class MenuData(
  val emoji: String,
  val title: String,
  val testTag: String,
  val toastMessage: String,
  val buttonType: ButtonType
)

@Composable
fun HomeMenuItem(
  emoji: String,
  title: String,
  testTag: String,
  buttonType: ButtonType,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Style properties based on Professional Polish Spec
  val containerColor = when (buttonType) {
    ButtonType.PRIMARY -> SaffronPrimary
    ButtonType.SECONDARY -> SaffronLight
    ButtonType.DEFAULT -> Slate50
  }

  val textColor = when (buttonType) {
    ButtonType.PRIMARY -> Color.White
    ButtonType.SECONDARY -> TextMedium
    ButtonType.DEFAULT -> TextMedium
  }

  val borderStroke = when (buttonType) {
    ButtonType.PRIMARY -> null
    ButtonType.SECONDARY -> BorderStroke(1.dp, SaffronBorder)
    ButtonType.DEFAULT -> BorderStroke(1.dp, Slate200)
  }

  val arrowColor = when (buttonType) {
    ButtonType.PRIMARY -> Color.White
    ButtonType.SECONDARY -> SaffronPrimary.copy(alpha = 0.8f)
    ButtonType.DEFAULT -> SaffronPrimary.copy(alpha = 0.6f)
  }

  val elevationDp = if (buttonType == ButtonType.PRIMARY) 4.dp else 0.dp

  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(
      containerColor = containerColor
    ),
    border = borderStroke,
    elevation = CardDefaults.cardElevation(defaultElevation = elevationDp),
    shape = RoundedCornerShape(24.dp), // rounded-3xl
    modifier = modifier
      .fillMaxWidth()
      .height(80.dp) // generous touch target and taller design matching HTML button padding
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start
    ) {
      // Emoji with generous spacing
      Text(
        text = emoji,
        fontSize = 24.sp,
        modifier = Modifier.align(Alignment.CenterVertically)
      )

      Spacer(modifier = Modifier.width(16.dp))

      // Gurmukhi Title Text
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 19.sp,
          color = textColor
        )
      )

      Spacer(modifier = Modifier.weight(1f))

      // Minimalist forward arrow
      Text(
        text = "→",
        fontSize = 22.sp,
        color = arrowColor,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
  MyApplicationTheme {
    HomeScreen(
      onNavigateToNitnem = {},
      onNavigateToSearch = {},
      onNavigateToBookmarks = {},
      onNavigateToSettings = {},
      onNavigateToAbout = {}
    )
  }
}

@Composable
fun TopAppBar(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .size(48.dp)
        .testTag("back_button")
    ) {
      Text(
        text = "←",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = SaffronPrimary
      )
    }
    
    Spacer(modifier = Modifier.width(12.dp))
    
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextMedium
      ),
      modifier = Modifier.testTag("app_bar_title")
    )
  }
}

@Composable
fun NitnemScreen(
  onBack: () -> Unit,
  onNavigateToBani: (String) -> Unit
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    visible = true
  }

  val banis = listOf(
    "ਜਪੁਜੀ ਸਾਹਿਬ",
    "ਜਾਪੁ ਸਾਹਿਬ",
    "ਤ੍ਵ ਪ੍ਰਸਾਦਿ ਸਵੱਯੇ",
    "ਚੌਪਈ ਸਾਹਿਬ",
    "ਅਨੰਦ ਸਾਹਿਬ",
    "ਰਹਿਰਾਸ ਸਾਹਿਬ",
    "ਅਰਦਾਸ",
    "ਕੀਰਤਨ ਸੋਹਿਲਾ",
    "ਆਰਤੀ",
    "ਆਸਾ ਦੀ ਵਾਰ",
    "ਸ੍ਰੀ ਸੁਖਮਨੀ ਸਾਹਿਬ"
  )

  val gurmukhiNumerals = listOf("੧", "੨", "੩", "੪", "੫", "੬", "੭", "੮", "੯", "੧੦", "੧੧")

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("nitnem_screen_scaffold"),
    containerColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      TopAppBar(
        title = "📖 ਨਿਤਨੇਮ ਸਾਹਿਬ",
        onBack = onBack,
        modifier = Modifier.padding(top = 8.dp)
      )

      AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring()) + slideInVertically(
          initialOffsetY = { 40 },
          animationSpec = spring()
        ),
        modifier = Modifier.weight(1f)
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("banis_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
          itemsIndexed(banis) { index, bani ->
            val numeral = gurmukhiNumerals.getOrElse(index) { "${index + 1}" }
            BaniCard(
              numeral = numeral,
              title = bani,
              testTag = "bani_item_${index}",
              onClick = { onNavigateToBani(bani) }
            )
          }
        }
      }

      // Bottom polish indicator spacer to match visual design
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(128.dp)
            .height(6.dp)
            .background(
              color = Slate200,
              shape = RoundedCornerShape(3.dp)
            )
        )
      }
    }
  }
}

@Composable
fun BaniCard(
  numeral: String,
  title: String,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(
      containerColor = Slate50
    ),
    border = BorderStroke(1.dp, Slate200),
    shape = RoundedCornerShape(20.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(72.dp)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Gurmukhi Numeral in custom colored circle
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(SaffronLight, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = numeral,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = SaffronDark,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      // Bani Name
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = TextMedium
        ),
        modifier = Modifier.weight(1f)
      )

      // Arrow indicator matching Polish style
      Text(
        text = "→",
        fontSize = 20.sp,
        color = SaffronPrimary.copy(alpha = 0.7f),
        fontWeight = FontWeight.Bold
      )
    }
  }
}

val santLipiFontFamily = FontFamily(
  Font(com.example.R.font.noto_serif_gurmukhi, FontWeight.Normal)
)

@Composable
fun buildGurmukhiLine(line: String, vishramColorHex: String): androidx.compose.ui.text.AnnotatedString {
  val color = try {
    if (vishramColorHex.isNotEmpty() && vishramColorHex != "None" && vishramColorHex != "none") {
      Color(android.graphics.Color.parseColor(vishramColorHex))
    } else {
      Color.Unspecified
    }
  } catch (e: Exception) {
    Color.Unspecified
  }

  if (color == Color.Unspecified) {
    return androidx.compose.ui.text.AnnotatedString(line)
  }

  return buildAnnotatedString {
    var startIndex = 0
    while (startIndex < line.length) {
      val dandaIndex = line.indexOf('।', startIndex)
      val doubleDandaIndex = line.indexOf('॥', startIndex)

      val nextIndex = when {
        dandaIndex >= 0 && doubleDandaIndex >= 0 -> minOf(dandaIndex, doubleDandaIndex)
        dandaIndex >= 0 -> dandaIndex
        doubleDandaIndex >= 0 -> doubleDandaIndex
        else -> -1
      }

      if (nextIndex == -1) {
        append(line.substring(startIndex))
        break
      }

      append(line.substring(startIndex, nextIndex))
      withStyle(style = SpanStyle(color = color)) {
        append(line[nextIndex])
      }
      startIndex = nextIndex + 1
    }
  }
}

@Composable
fun BaniDetailScreen(
  baniName: String,
  highlightIndex: Int? = null,
  viewModel: BookmarksViewModel,
  settingsManager: SettingsManager,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val settingsState by settingsManager.settings.collectAsStateWithLifecycle()

  val fontSize = when (settingsState.fontSize) {
    "Small" -> 15.sp
    "Large" -> 23.sp
    "Extra Large" -> 28.sp
    else -> 19.sp // Medium
  }

  val fontFamily = when (settingsState.fontFamily) {
    "Sant Lipi" -> santLipiFontFamily
    "Unicode Gurmukhi" -> FontFamily.SansSerif
    else -> FontFamily.Default
  }

  val lineSpacingMultiplier = settingsState.lineSpacing
  val lineHeight = fontSize * lineSpacingMultiplier

  val bani = remember(baniName) {
    val fileName = getBaniFileName(baniName)
    if (fileName.isNotEmpty()) {
      loadBaniFromAsset(context, fileName)
    } else {
      Bani("", baniName, emptyList())
    }
  }

  val listState = rememberLazyListState()

  // Last read position handling
  val lastReadPosFlow = remember(bani.fileName) {
    settingsManager.getLastReadPosition(bani.fileName)
  }
  val lastReadIndex by lastReadPosFlow.collectAsStateWithLifecycle(initialValue = 0)

  // Scroll to highlightIndex if present, otherwise restore last read position
  var hasInitialScrolled by remember(bani.fileName) { mutableStateOf(false) }
  LaunchedEffect(bani.fileName, highlightIndex, lastReadIndex) {
    if (!hasInitialScrolled && bani.verses.isNotEmpty()) {
      val targetIndex = when {
        highlightIndex != null && highlightIndex >= 0 && highlightIndex < bani.verses.size -> highlightIndex
        lastReadIndex > 0 && lastReadIndex < bani.verses.size -> lastReadIndex
        else -> 0
      }
      if (targetIndex > 0) {
        listState.scrollToItem(targetIndex)
      }
      hasInitialScrolled = true
    }
  }

  // Save last read position as user scrolls
  LaunchedEffect(listState, bani.fileName) {
    snapshotFlow { listState.firstVisibleItemIndex }
      .collect { firstIndex ->
        if (hasInitialScrolled && firstIndex >= 0 && bani.fileName.isNotEmpty()) {
          settingsManager.saveLastReadPosition(bani.fileName, firstIndex)
        }
      }
  }

  val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("bani_detail_screen_scaffold"),
    containerColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      TopAppBar(
        title = baniName,
        onBack = onBack,
        modifier = Modifier.padding(top = 8.dp)
      )

      if (bani.verses.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = SaffronLight
            ),
            border = BorderStroke(1.dp, SaffronBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
              .testTag("empty_bani_card")
          ) {
            Column(
              modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Text(
                text = "ੴ",
                style = MaterialTheme.typography.displayMedium.copy(
                  fontSize = 48.sp,
                  color = SaffronPrimary,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
              )

              Text(
                text = "ਵਾਹਿਗੁਰੂ ਜੀ ਕਾ ਖਾਲਸਾ\nਵਾਹਿਗੁਰੂ ਜੀ ਕੀ ਫਤਿਹ॥",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = TextMedium,
                  textAlign = TextAlign.Center,
                  lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
              )

              Text(
                text = "ਇਸ ਪਵਿੱਤਰ ਬਾਣੀ ਦਾ ਪਾਠ ਅਗਲੇ ਅਪਡੇਟ ਵਿੱਚ ਜਲਦੀ ਹੀ ਉਪਲਬਧ ਕਰਵਾਇਆ ਜਾਵੇਗਾ।",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextGray,
                  fontSize = 14.sp,
                  textAlign = TextAlign.Center,
                  lineHeight = 20.sp
                )
              )
            }
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("bani_verses_list"),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
          itemsIndexed(bani.verses, key = { index, _ -> index }) { index, verse ->
            val isHighlighted = index == highlightIndex
            val isBookmarked = bookmarks.any { it.fileName == bani.fileName && it.lineIndex == index }

            Card(
              colors = CardDefaults.cardColors(
                containerColor = if (isHighlighted) SaffronLight else Slate50
              ),
              border = BorderStroke(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = if (isHighlighted) SaffronPrimary else Slate200
              ),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("verse_card_$index")
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 80.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  // Gurmukhi verse line
                  Text(
                    text = buildGurmukhiLine(verse.line, settingsState.vishramColor),
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = fontSize,
                      fontFamily = fontFamily,
                      color = TextMedium,
                      textAlign = TextAlign.Center,
                      lineHeight = lineHeight
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("verse_line_$index")
                  )

                  if (verse.translation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // English Translation line
                    Text(
                      text = verse.translation,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verse_translation_$index")
                    )
                  }
                }

                // Action buttons: Copy, Share, Bookmark
                Row(
                  modifier = Modifier
                    .align(Alignment.TopEnd),
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Copy Verse Button
                  IconButton(
                    onClick = {
                      val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                      val textToCopy = if (verse.translation.isNotEmpty()) "${verse.line}\n${verse.translation}" else verse.line
                      val clip = android.content.ClipData.newPlainText("Gurbani Verse", textToCopy)
                      clipboard.setPrimaryClip(clip)
                      Toast.makeText(context, "ਤੁਕ ਕੋਪੀ ਹੋ ਗਈ॥", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                      .size(32.dp)
                      .testTag("copy_icon_$index")
                  ) {
                    Text(text = "📋", fontSize = 15.sp)
                  }

                  // Share Verse Button
                  IconButton(
                    onClick = {
                      val shareText = if (verse.translation.isNotEmpty()) {
                        "${verse.line}\n${verse.translation}\n\n— ਗੁਰਬਾਣੀ ਖੋਜ ($baniName)"
                      } else {
                        "${verse.line}\n\n— ਗੁਰਬਾਣੀ ਖੋਜ ($baniName)"
                      }
                      val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                      }
                      context.startActivity(Intent.createChooser(shareIntent, "ਤੁਕ ਸਾਂਝੀ ਕਰੋ"))
                    },
                    modifier = Modifier
                      .size(32.dp)
                      .testTag("share_icon_$index")
                  ) {
                    Text(text = "📤", fontSize = 15.sp)
                  }

                  // Bookmark Icon Button
                  IconButton(
                    onClick = {
                      viewModel.toggleBookmark(
                        baniName = baniName,
                        fileName = bani.fileName,
                        lineIndex = index,
                        verseLine = verse.line,
                        translation = verse.translation
                      ) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier
                      .size(32.dp)
                      .testTag("bookmark_icon_$index")
                  ) {
                    Text(
                      text = if (isBookmarked) "⭐" else "☆",
                      fontSize = 18.sp
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Bottom gesture line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(128.dp)
            .height(6.dp)
            .background(
              color = Slate200,
              shape = RoundedCornerShape(3.dp)
            )
        )
      }
    }
  }
}

@Composable
fun SearchScreen(
  onBack: () -> Unit,
  onNavigateToBani: (String, Int) -> Unit
) {
  val context = LocalContext.current
  var allBanis by remember { mutableStateOf<List<Bani>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

  val baniFiles = listOf(
    "japji_sahib.json",
    "jaap_sahib.json",
    "tav_prasad_savaiye.json",
    "chaupai_sahib.json",
    "anand_sahib.json",
    "rehras_sahib.json",
    "ardas.json",
    "kirtan_sohila.json",
    "aarti.json",
    "asa_di_vaar.json",
    "sri_sukhmani_sahib.json"
  )

  LaunchedEffect(Unit) {
    allBanis = baniFiles.map { fileName ->
      loadBaniFromAsset(context, fileName)
    }
  }

  LaunchedEffect(searchQuery) {
    if (searchQuery.isBlank()) {
      searchResults = emptyList()
    } else {
      val query = searchQuery.trim()
      val results = mutableListOf<SearchResult>()
      allBanis.forEach { bani ->
        bani.verses.forEach { verse ->
          if (verse.line.contains(query, ignoreCase = true) || 
              verse.translation.contains(query, ignoreCase = true)) {
            results.add(SearchResult(baniName = bani.title, fileName = bani.fileName, verse = verse))
          }
        }
      }
      searchResults = results
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("search_screen_scaffold"),
    containerColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      TopAppBar(
        title = "🔍 ਗੁਰਬਾਣੀ ਖੋਜ",
        onBack = onBack,
        modifier = Modifier.padding(top = 8.dp)
      )

      // OutlinedTextField for searching
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            text = "ਇੱਥੇ ਅੱਖਰ ਜਾਂ ਸ਼ਬਦ ਲਿਖੋ...",
            color = TextGray,
            fontSize = 15.sp
          )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Slate50,
          unfocusedContainerColor = Slate50,
          focusedTextColor = TextMedium,
          unfocusedTextColor = TextMedium,
          focusedIndicatorColor = SaffronPrimary,
          unfocusedIndicatorColor = Slate200,
          cursorColor = SaffronPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp)
          .testTag("search_input_field")
      )

      if (searchQuery.isBlank()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "ੴ",
            style = MaterialTheme.typography.displayMedium.copy(
              fontSize = 36.sp,
              color = SaffronPrimary.copy(alpha = 0.5f),
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 12.dp)
          )
          Text(
            text = "ਗੁਰਮੁਖੀ ਅੱਖਰਾਂ ਵਿੱਚ ਟਾਈਪ ਕਰੋ\nਜਿਵੇਂ ਕਿ 'ਸਤਿਗੁਰ' ਜਾਂ 'ਅਨੰਦੁ'",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextGray,
              fontSize = 14.sp,
              textAlign = TextAlign.Center,
              lineHeight = 20.sp
            ),
            modifier = Modifier.testTag("search_empty_hint")
          )
        }
      } else if (searchResults.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "ਕੋਈ ਨਤੀਜਾ ਨਹੀਂ ਮਿਲਿਆ",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextMedium,
              fontSize = 16.sp
            ),
            modifier = Modifier.testTag("search_no_results_text")
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "ਕਿਰਪਾ ਕਰਕੇ ਕੋਈ ਹੋਰ ਸ਼ਬਦ ਲਿਖੋ।",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextGray,
              fontSize = 13.sp
            )
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("search_results_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
        ) {
          itemsIndexed(searchResults) { index, result ->
            Card(
              onClick = { onNavigateToBani(result.baniName, result.verse.index) },
              colors = CardDefaults.cardColors(
                containerColor = Slate50
              ),
              border = BorderStroke(1.dp, Slate200),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("search_result_card_$index")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                // Bani Name Pill
                Box(
                  modifier = Modifier
                    .background(SaffronLight, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = result.baniName,
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = SaffronDark,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    ),
                    modifier = Modifier.testTag("search_result_bani_name_$index")
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Highlighted Gurmukhi Verse
                HighlightedText(
                  text = result.verse.line,
                  query = searchQuery,
                  highlightColor = SaffronPrimary,
                  textColor = TextMedium,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )

                if (result.verse.translation.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  // Highlighted Translation
                  HighlightedText(
                    text = result.verse.translation,
                    query = searchQuery,
                    highlightColor = SaffronPrimary.copy(alpha = 0.8f),
                    textColor = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                  )
                }
              }
            }
          }
        }
      }

      // Bottom gesture line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(128.dp)
            .height(6.dp)
            .background(
              color = Slate200,
              shape = RoundedCornerShape(3.dp)
            )
        )
      }
    }
  }
}

@Composable
fun HighlightedText(
  text: String,
  query: String,
  highlightColor: Color = SaffronPrimary,
  textColor: Color = TextDark,
  fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
  fontWeight: FontWeight = FontWeight.Normal
) {
  val annotatedString = remember(text, query) {
    buildAnnotatedString {
      if (query.isEmpty() || !text.contains(query, ignoreCase = true)) {
        append(text)
      } else {
        var startIndex = 0
        while (true) {
          val index = text.indexOf(query, startIndex, ignoreCase = true)
          if (index == -1) {
            append(text.substring(startIndex))
            break
          }
          append(text.substring(startIndex, index))
          withStyle(style = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
            append(text.substring(index, index + query.length))
          }
          startIndex = index + query.length
        }
      }
    }
  }

  Text(
    text = annotatedString,
    fontSize = fontSize,
    color = textColor,
    fontWeight = fontWeight
  )
}

@Composable
fun BookmarksScreen(
  viewModel: BookmarksViewModel,
  onBack: () -> Unit,
  onNavigateToBani: (String, Int) -> Unit
) {
  val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  val filteredBookmarks = remember(bookmarks, searchQuery) {
    if (searchQuery.isBlank()) {
      bookmarks
    } else {
      bookmarks.filter {
        it.verseLine.contains(searchQuery, ignoreCase = true) ||
        it.baniName.contains(searchQuery, ignoreCase = true) ||
        it.translation.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
          val jsonArray = org.json.JSONArray()
          bookmarks.forEach { b ->
            val obj = org.json.JSONObject()
            obj.put("baniName", b.baniName)
            obj.put("fileName", b.fileName)
            obj.put("lineIndex", b.lineIndex)
            obj.put("verseLine", b.verseLine)
            obj.put("translation", b.translation)
            obj.put("timestamp", b.timestamp)
            jsonArray.put(obj)
          }
          outputStream.write(jsonArray.toString(2).toByteArray(Charsets.UTF_8))
        }
        Toast.makeText(context, "ਬੁੱਕਮਾਰਕ ਨਿਰਯਾਤ (Export) ਹੋ ਗਏ॥", Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Export ਵਿੱਚ ਗਲਤੀ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      try {
        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (!jsonString.isNullOrEmpty()) {
          val jsonArray = org.json.JSONArray(jsonString)
          val importedList = mutableListOf<Bookmark>()
          for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            importedList.add(
              Bookmark(
                baniName = obj.optString("baniName", ""),
                fileName = obj.optString("fileName", ""),
                lineIndex = obj.optInt("lineIndex", 0),
                verseLine = obj.optString("verseLine", ""),
                translation = obj.optString("translation", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
              )
            )
          }
          if (importedList.isNotEmpty()) {
            viewModel.importBookmarks(importedList) { count ->
              Toast.makeText(context, "$count ਬੁੱਕਮਾਰਕ ਆਯਾਤ (Import) ਹੋ ਗਏ॥", Toast.LENGTH_LONG).show()
            }
          } else {
            Toast.makeText(context, "ਫਾਈਲ ਵਿੱਚ ਕੋਈ ਬੁੱਕਮਾਰਕ ਨਹੀਂ ਮਿਲਿਆ।", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Import ਵਿੱਚ ਗਲਤੀ: JSON ਫਾਈਲ ਦਾ ਫਾਰਮੈਟ ਸਹੀ ਨਹੀਂ ਹੈ।", Toast.LENGTH_LONG).show()
      }
    }
  }

  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("ਬੁੱਕਮਾਰਕ ਹਟਾਓ", fontWeight = FontWeight.Bold) },
      text = { Text("ਕੀ ਤੁਸੀਂ ਸਾਰੇ ਬੁੱਕਮਾਰਕ ਸਾਫ਼ ਕਰਨਾ ਚਾਹੁੰਦੇ ਹੋ?") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.clearAllBookmarks {
              Toast.makeText(context, "ਸਾਰੇ ਬੁੱਕਮਾਰਕ ਸਾਫ਼ ਹੋ ਗਏ॥", Toast.LENGTH_SHORT).show()
            }
            showClearConfirmDialog = false
          },
          modifier = Modifier.testTag("confirm_clear_bookmarks")
        ) {
          Text("ਹਾਂ, ਸਾਫ਼ ਕਰੋ", color = Color.Red, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("ਰੱਦ ਕਰੋ")
        }
      }
    )
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("bookmarks_screen_scaffold"),
    containerColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      TopAppBar(
        title = "⭐ ਬੁੱਕਮਾਰਕਸ",
        onBack = onBack,
        modifier = Modifier.padding(top = 8.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Action buttons row: Export, Import, Clear
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Card(
          onClick = {
            if (bookmarks.isEmpty()) {
              Toast.makeText(context, "ਨਿਰਯਾਤ ਕਰਨ ਲਈ ਕੋਈ ਬੁੱਕਮਾਰਕ ਨਹੀਂ ਹੈ।", Toast.LENGTH_SHORT).show()
            } else {
              exportLauncher.launch("gurbani_bookmarks_${System.currentTimeMillis()}.json")
            }
          },
          colors = CardDefaults.cardColors(containerColor = SaffronLight),
          border = BorderStroke(1.dp, SaffronPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(40.dp).testTag("export_bookmarks_btn")
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("📤 Export", color = SaffronDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Card(
          onClick = { importLauncher.launch("application/json") },
          colors = CardDefaults.cardColors(containerColor = Slate50),
          border = BorderStroke(1.dp, Slate200),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(40.dp).testTag("import_bookmarks_btn")
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("📥 Import", color = TextMedium, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        if (bookmarks.isNotEmpty()) {
          Card(
            onClick = { showClearConfirmDialog = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
            border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(40.dp).testTag("clear_bookmarks_btn")
          ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("🗑️ ਸਾਫ਼ ਕਰੋ", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (bookmarks.isNotEmpty()) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("ਬੁੱਕਮਾਰਕ ਖੋਜੋ...", color = TextGray, fontSize = 14.sp) },
          leadingIcon = { Text("🔍", fontSize = 14.sp) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Text("✕", fontSize = 12.sp, color = TextGray)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = TextFieldDefaults.colors(
            focusedContainerColor = Slate50,
            unfocusedContainerColor = Slate50,
            focusedIndicatorColor = SaffronPrimary,
            unfocusedIndicatorColor = Slate200
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("bookmarks_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))
      }

      if (bookmarks.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "☆",
            style = MaterialTheme.typography.displayMedium.copy(
              fontSize = 48.sp,
              color = SaffronPrimary.copy(alpha = 0.5f),
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 12.dp)
          )
          Text(
            text = "ਕੋਈ ਬੁੱਕਮਾਰਕ ਨਹੀਂ ਮਿਲਿਆ।\nਪਾਠ ਕਰਦੇ ਸਮੇਂ ਲਾਈਨ ਦੇ ਨਾਲ ਦਿੱਤੇ ⭐ ਆਈਕਨ 'ਤੇ ਟੈਪ ਕਰਕੇ ਬੁੱਕਮਾਰਕ ਕਰੋ।\nਜਾਂ 'Import' ਰਾਹੀਂ ਬੁੱਕਮਾਰਕ ਫਾਈਲ ਸ਼ਾਮਲ ਕਰੋ।",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextGray,
              fontSize = 14.sp,
              textAlign = TextAlign.Center,
              lineHeight = 22.sp
            ),
            modifier = Modifier.testTag("bookmarks_empty_hint")
          )
        }
      } else if (filteredBookmarks.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "'$searchQuery' ਲਈ ਕੋਈ ਬੁੱਕਮਾਰਕ ਨਹੀਂ ਮਿਲਿਆ।",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextGray,
              fontSize = 14.sp,
              textAlign = TextAlign.Center
            ),
            modifier = Modifier.testTag("bookmarks_no_search_results")
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("bookmarks_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
        ) {
          itemsIndexed(filteredBookmarks, key = { _, b -> "${b.fileName}_${b.lineIndex}_${b.id}" }) { index, bookmark ->
            Card(
              onClick = { onNavigateToBani(bookmark.baniName, bookmark.lineIndex) },
              colors = CardDefaults.cardColors(containerColor = Slate50),
              border = BorderStroke(1.dp, Slate200),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("bookmark_card_$index")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .background(SaffronLight, shape = RoundedCornerShape(8.dp))
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = bookmark.baniName,
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = SaffronDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      ),
                      modifier = Modifier.testTag("bookmark_bani_name_$index")
                    )
                  }

                  IconButton(
                    onClick = {
                      viewModel.toggleBookmark(
                        baniName = bookmark.baniName,
                        fileName = bookmark.fileName,
                        lineIndex = bookmark.lineIndex,
                        verseLine = bookmark.verseLine,
                        translation = bookmark.translation
                      ) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier.size(28.dp).testTag("delete_bookmark_$index")
                  ) {
                    Text(text = "❌", fontSize = 12.sp)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HighlightedText(
                  text = bookmark.verseLine,
                  query = searchQuery,
                  textColor = TextMedium,
                  highlightColor = SaffronPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )

                if (bookmark.translation.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  HighlightedText(
                    text = bookmark.translation,
                    query = searchQuery,
                    textColor = TextGray,
                    highlightColor = SaffronPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                  )
                }
              }
            }
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(128.dp)
            .height(6.dp)
            .background(color = Slate200, shape = RoundedCornerShape(3.dp))
        )
      }
    }
  }
}

@Composable
fun WelcomeScreen(
  onGetStarted: () -> Unit
) {
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    visible = true
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("welcome_screen_scaffold"),
    containerColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 24.dp)
      ) {
        AnimatedVisibility(
          visible = visible,
          enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { -40 }, animationSpec = spring())
        ) {
          Text(
            text = "ੴ",
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = 80.sp,
              color = SaffronPrimary,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.testTag("welcome_ek_onkar")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
          visible = visible,
          enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { 40 }, animationSpec = spring())
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "ਜੀ ਆਇਆਂ ਨੂੰ - ਗੁਰਬਾਣੀ ਖੋਜ",
              style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = TextMedium,
                textAlign = TextAlign.Center
              ),
              modifier = Modifier.testTag("welcome_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "ਸ੍ਰੀ ਗੁਰੂ ਗ੍ਰੰਥ ਸਾਹਿਬ ਜੀ ਅਤੇ ਨਿਤਨੇਮ ਬਾਣੀਆਂ ਦਾ ਸਰਲ ਸੰਚਾਰ",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
              ),
              modifier = Modifier.testTag("welcome_subtitle")
            )
          }
        }
      }

      AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = 100f)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = spring(stiffness = 100f)),
        modifier = Modifier.weight(1f).padding(vertical = 16.dp)
      ) {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
          item {
            WelcomeFeatureRow(
              emoji = "📖",
              title = "ਸੰਪੂਰਨ ਨਿਤਨੇਮ ਬਾਣੀਆਂ",
              desc = "ਜਪੁਜੀ ਸਾਹਿਬ, ਜਾਪੁ ਸਾਹਿਬ, ਚੌਪਈ ਸਾਹਿਬ, ਅਨੰਦ ਸਾਹਿਬ, ਰਹਿਰਾਸ ਸਾਹਿਬ ਤੇ ਹੋਰ ਸਾਰੀਆਂ ਪਾਵਨ ਬਾਣੀਆਂ।"
            )
          }
          item {
            WelcomeFeatureRow(
              emoji = "🔍",
              title = "ਤੀਵਰ ਗੁਰਬਾਣੀ ਖੋਜ",
              desc = "ਪਹਿਲੇ ਅੱਖਰ ਨਾਲ ਜਾਂ ਪੂਰੇ ਸ਼ਬਦ ਨਾਲ ਆਫਲਾਈਨ ਖੋਜ ਕਰੋ।"
            )
          }
          item {
            WelcomeFeatureRow(
              emoji = "🎨",
              title = "ਅਨੁਕੂਲ ਪਾਠ ਅਨੁਭਵ",
              desc = "ਫੋਂਟ ਅਕਾਰ, ਪੁਰਾਤਨ ਸੁੰਦਰ ਲਿਪੀਆਂ (ਸੰਤ ਲਿਪੀ), ਲਾਈਨਾਂ ਦੀ ਵਿੱਥ ਅਤੇ ਵਿਸ਼ਰਾਮ ਹਾਈਲਾਈਟ।"
            )
          }
          item {
            WelcomeFeatureRow(
              emoji = "💾",
              title = "ਬੁੱਕਮਾਰਕਸ ਤੇ ਬੈਕਅੱਪ",
              desc = "100% ਆਫਲਾਈਨ। ਆਪਣੇ ਮਨਪਸੰਦ ਸ਼ਬਦ ਸੇਵ ਕਰੋ ਅਤੇ JSON ਰਾਹੀਂ Export/Import ਕਰੋ।"
            )
          }
        }
      }

      Card(
        onClick = onGetStarted,
        colors = CardDefaults.cardColors(containerColor = SaffronPrimary),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("get_started_btn")
      ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = "ਸ਼ੁਰੂ ਕਰੋ (Get Started)",
            style = MaterialTheme.typography.titleMedium.copy(
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
          )
        }
      }
    }
  }
}

@Composable
fun WelcomeFeatureRow(emoji: String, title: String, desc: String) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Slate50),
    border = BorderStroke(1.dp, Slate200),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
      Column {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextMedium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = desc, fontSize = 12.sp, color = TextGray, lineHeight = 16.sp)
      }
    }
  }
}

@Composable
fun AboutScreen(
  onBack: () -> Unit
) {
  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("about_screen_scaffold"),
    containerColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      TopAppBar(
        title = "ℹ️ ਐਪ ਬਾਰੇ (About)",
        onBack = onBack,
        modifier = Modifier.padding(top = 8.dp)
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f)
          .testTag("about_content_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
      ) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = SaffronLight),
            border = BorderStroke(1.dp, SaffronBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("about_header_card")
          ) {
            Column(
              modifier = Modifier.padding(20.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "ੴ",
                style = MaterialTheme.typography.displayMedium.copy(
                  fontSize = 48.sp,
                  color = SaffronPrimary,
                  fontWeight = FontWeight.Bold
                )
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "ਗੁਰਬਾਣੀ ਖੋਜ (Gurbani Khoj)",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = TextMedium,
                  fontSize = 20.sp
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "ਸੰਸਕਰਣ: 1.0.0 (Version 1.0.0)",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = SaffronDark,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Made by Manjot Singh M.Aa✶",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = TextMedium,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                ),
                modifier = Modifier.testTag("made_by_credit")
              )
            }
          }
        }

        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("about_quote_card")
          ) {
            Column(
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "“ ਗੁਰਬਾਣੀ ਇਸੁ ਜਗ ਮਹਿ ਚਾਨਣੁ ਕਰਮਿ ਵਸੈ ਮਨਿ ਆਏ ॥ ”",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = SaffronPrimary,
                  textAlign = TextAlign.Center,
                  lineHeight = 22.sp
                )
              )
            }
          }
        }

        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("about_details_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ਮੁੱਖ ਵਿਸ਼ੇਸ਼ਤਾਵਾਂ (Key Features)",
                fontWeight = FontWeight.Bold,
                color = TextMedium,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "• 100% ਆਫਲਾਈਨ (No Internet Required)\n" +
                       "• ਸੰਪੂਰਨ ਨਿਤਨੇਮ ਅਤੇ ਸੁੰਦਰ ਗੁਰਬਾਣੀ ਲਾਇਬ੍ਰੇਰੀ\n" +
                       "• ਪਹਿਲੇ ਅੱਖਰਾਂ ਨਾਲ ਤੀਵਰ ਖੋਜ ਪ੍ਰਣਾਲੀ\n" +
                       "• ਅਨੁਕੂਲ ਫੋਂਟ, ਲਾਈਨ ਸਪੇਸਿੰਗ ਅਤੇ ਵਿਸ਼ਰਾਮ ਹਾਈਲਾਈਟ\n" +
                       "• ਬੁੱਕਮਾਰਕਸ JSON Export/Import ਬੈਕਅੱਪ\n" +
                       "• ਡਾਰਕ, ਲਾਈਟ ਅਤੇ ਸਿਸਟਮ ਥੀਮ ਸਪੋਰਟ",
                fontSize = 13.sp,
                color = TextMedium,
                lineHeight = 22.sp
              )
            }
          }
        }

        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("about_privacy_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ਸੁਰੱਖਿਆ ਅਤੇ ਨਿੱਜਤਾ (Privacy & Security)",
                fontWeight = FontWeight.Bold,
                color = TextMedium,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "ਇਹ ਐਪ ਪੂਰੀ ਤਰ੍ਹਾਂ ਨਿੱਜੀ ਹੈ। ਕੋਈ ਵੀ ਵਿਗਿਆਪਨ (Ads) ਨਹੀਂ ਹਨ ਅਤੇ ਕੋਈ ਵੀ ਨਿੱਜੀ ਡਾਟਾ ਇਕੱਠਾ ਨਹੀਂ ਕੀਤਾ ਜਾਂਦਾ। ਸਾਰਾ ਡਾਟਾ ਤੁਹਾਡੇ ਡਿਵਾਈਸ 'ਤੇ ਸੁਰੱਖਿਅਤ ਰਹਿੰਦਾ ਹੈ।",
                fontSize = 12.sp,
                color = TextGray,
                lineHeight = 18.sp
              )
            }
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(128.dp)
            .height(6.dp)
            .background(color = Slate200, shape = RoundedCornerShape(3.dp))
        )
      }
    }
  }
}

@Composable
fun SettingsScreen(
  settingsManager: SettingsManager,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val settingsState by settingsManager.settings.collectAsStateWithLifecycle()

  val previewFontSize = when (settingsState.fontSize) {
    "Small" -> 15.sp
    "Large" -> 23.sp
    "Extra Large" -> 28.sp
    else -> 19.sp // Medium
  }

  val previewFontFamily = when (settingsState.fontFamily) {
    "Sant Lipi" -> santLipiFontFamily
    "Unicode Gurmukhi" -> FontFamily.SansSerif
    else -> FontFamily.Default
  }

  val previewLineHeight = previewFontSize * settingsState.lineSpacing

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("settings_screen_scaffold"),
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      TopAppBar(
        title = "ਸੈਟਿੰਗਾਂ",
        onBack = onBack,
        modifier = Modifier.padding(top = 8.dp)
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f)
          .testTag("settings_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
      ) {
        // 1. Live Preview Section
        item {
          Text(
            text = "ਪੂਰਵਦਰਸ਼ਨ (Preview)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SaffronPrimary
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E1E1E) else Slate50
            ),
            border = BorderStroke(1.dp, SaffronBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("settings_preview_card")
          ) {
            Column(
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = buildGurmukhiLine("ੴ ਸਤਿ ਨਾਮੁ ਕਰਤਾ ਪੁਰਖੁ ਨਿਰਭਉ ਨਿਰਵੈਰੁ ॥", settingsState.vishramColor),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = previewFontSize,
                  fontFamily = previewFontFamily,
                  color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else TextMedium,
                  textAlign = TextAlign.Center,
                  lineHeight = previewLineHeight
                ),
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "One Universal Creator God. Truth is the Name. Creator Being Personified.",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  color = TextGray,
                  textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        // 2. Reading Experience Section
        item {
          Text(
            text = "ਪਾਠ ਅਨੁਭਵ (Reading Experience)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SaffronPrimary
            )
          )
        }

        // Font Size Picker
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("font_size_setting_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ਅੱਖਰਾਂ ਦਾ ਅਕਾਰ (Font Size)",
                fontWeight = FontWeight.Bold,
                color = TextMedium,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("Small", "Medium", "Large", "Extra Large").forEach { size ->
                  val isSelected = settingsState.fontSize == size
                  Card(
                    onClick = { settingsManager.updateFontSize(size) },
                    colors = CardDefaults.cardColors(
                      containerColor = if (isSelected) SaffronPrimary else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Slate200),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("font_size_btn_$size")
                  ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      Text(
                        text = when(size) {
                          "Small" -> "ਛੋਟਾ"
                          "Medium" -> "ਦਰਮਿਆਨਾ"
                          "Large" -> "ਵੱਡਾ"
                          else -> "ਬਹੁਤ ਵੱਡਾ"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = if (isSelected) Color.White else TextMedium,
                          fontSize = 12.sp
                        )
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Font Family Picker
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("font_family_setting_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ਫੋਂਟ ਸ਼ੈਲੀ (Font Family)",
                fontWeight = FontWeight.Bold,
                color = TextMedium,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("Default Gurmukhi", "Sant Lipi", "Unicode Gurmukhi").forEach { family ->
                  val isSelected = settingsState.fontFamily == family
                  Card(
                    onClick = { settingsManager.updateFontFamily(family) },
                    colors = CardDefaults.cardColors(
                      containerColor = if (isSelected) SaffronLight else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Slate200),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("font_family_btn_$family")
                  ) {
                    Row(
                      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = when(family) {
                          "Default Gurmukhi" -> "ਸਿਸਟਮ ਫੋਂਟ (Default Gurmukhi)"
                          "Sant Lipi" -> "ਪੁਰਾਤਨ ਲਿਪੀ (Sant Lipi)"
                          else -> "ਯੂਨੀਕੋਡ (Unicode Gurmukhi)"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = TextMedium,
                          fontFamily = if (family == "Sant Lipi") santLipiFontFamily else if (family == "Unicode Gurmukhi") FontFamily.SansSerif else FontFamily.Default
                        )
                      )
                      if (isSelected) {
                        Text(text = "✓", color = SaffronPrimary, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Line Spacing Slider
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("line_spacing_setting_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "ਲਾਈਨਾਂ ਵਿਚਲੀ ਵਿੱਥ (Line Spacing)",
                  fontWeight = FontWeight.Bold,
                  color = TextMedium,
                  fontSize = 15.sp
                )
                Text(
                  text = String.format("%.1fx", settingsState.lineSpacing),
                  fontWeight = FontWeight.Bold,
                  color = SaffronPrimary,
                  fontSize = 14.sp
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              androidx.compose.material3.Slider(
                value = settingsState.lineSpacing,
                onValueChange = { settingsManager.updateLineSpacing(it) },
                valueRange = 1.0f..2.5f,
                steps = 14,
                colors = androidx.compose.material3.SliderDefaults.colors(
                  thumbColor = SaffronPrimary,
                  activeTrackColor = SaffronPrimary,
                  inactiveTrackColor = Slate200
                ),
                modifier = Modifier.testTag("line_spacing_slider")
              )
            }
          }
        }

        // 3. Theme & Keep Screen On Section
        item {
          Text(
            text = "ਦਿੱਖ ਅਤੇ ਥੀਮ (Appearance)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SaffronPrimary
            )
          )
        }

        // Theme selector
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("theme_setting_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ਐਪ ਥੀਮ (Theme)",
                fontWeight = FontWeight.Bold,
                color = TextMedium,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("System", "Light", "Dark").forEach { mode ->
                  val isSelected = settingsState.themeMode == mode
                  Card(
                    onClick = { settingsManager.updateThemeMode(mode) },
                    colors = CardDefaults.cardColors(
                      containerColor = if (isSelected) SaffronPrimary else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Slate200),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("theme_btn_$mode")
                  ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      Text(
                        text = when(mode) {
                          "System" -> "ਸਿਸਟਮ"
                          "Light" -> "ਲਾਈਟ"
                          else -> "ਡਾਰਕ"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = if (isSelected) Color.White else TextMedium,
                          fontSize = 12.sp
                        )
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Keep Screen On Switch
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("keep_screen_on_setting_card")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "ਸਕ੍ਰੀਨ ਚਾਲੂ ਰੱਖੋ (Keep Screen On)",
                  fontWeight = FontWeight.Bold,
                  color = TextMedium,
                  fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "ਪਾਠ ਕਰਦੇ ਸਮੇਂ ਸਕ੍ਰੀਨ ਬੰਦ ਨਹੀਂ ਹੋਵੇਗੀ",
                  color = TextGray,
                  fontSize = 12.sp
                )
              }
              androidx.compose.material3.Switch(
                checked = settingsState.keepScreenOn,
                onCheckedChange = { settingsManager.updateKeepScreenOn(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = SaffronPrimary,
                  uncheckedThumbColor = Slate200,
                  uncheckedTrackColor = Color.White
                ),
                modifier = Modifier.testTag("keep_screen_on_switch")
              )
            }
          }
        }

        // 4. Vishram Highlight Section
        item {
          Text(
            text = "ਵਿਸ਼ਰਾਮ ਹਾਈਲਾਈਟ (Vishram Color)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SaffronPrimary
            )
          )
        }

        // Vishram color picker
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("vishram_color_setting_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "ਹਾਈਲਾਈਟ ਰੰਗ (Highlight Color)",
                fontWeight = FontWeight.Bold,
                color = TextMedium,
                fontSize = 15.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val colorOptions = listOf(
                  "#FF9933" to "Saffron",
                  "#2E7D32" to "Green",
                  "#1565C0" to "Blue",
                  "#C62828" to "Red",
                  "#6A1B9A" to "Purple",
                  "None" to "None"
                )

                colorOptions.forEach { (colorHex, name) ->
                  val isSelected = settingsState.vishramColor == colorHex
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(40.dp)
                      .clip(androidx.compose.foundation.shape.CircleShape)
                      .background(
                        if (colorHex == "None") Color.Transparent else Color(android.graphics.Color.parseColor(colorHex)),
                        shape = androidx.compose.foundation.shape.CircleShape
                      )
                      .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) SaffronPrimary else if (colorHex == "None") TextGray else Color.Transparent,
                        shape = androidx.compose.foundation.shape.CircleShape
                      )
                      .clickable { settingsManager.updateVishramColor(colorHex) }
                      .testTag("vishram_color_btn_$name")
                  ) {
                    if (colorHex == "None") {
                      Text(text = "✕", color = TextGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else if (isSelected) {
                      Text(text = "✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Bottom gesture line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(128.dp)
            .height(6.dp)
            .background(
              color = Slate200,
              shape = RoundedCornerShape(3.dp)
            )
        )
      }
    }
  }
}

