package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// --- Data Models ---
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val description: String,
    val category: String
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class Order(
    val id: String,
    val buyerName: String,
    val address: String,
    val phone: String,
    val email: String,
    val items: List<CartItem>,
    val total: Double,
    val status: String = "Pending"
)

// --- Mock Data ---
val initialProducts = listOf(
    Product("1", "Nike Air Max", 4500.0, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80", "Classic design meets modern comfort. These sneakers feature premium materials and signature cushioning.", "Shoes"),
    Product("2", "Minimalist Watch", 2500.0, "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80", "An elegant timepiece that blends perfectly with any outfit. Features a scratch-resistant face and genuine leather strap.", "Accessories"),
    Product("3", "Sony On-Ear", 7999.0, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80", "Immerse yourself in high-fidelity audio with industry-leading noise cancellation technology.", "Electronics"),
    Product("4", "Eames Chair", 14999.0, "https://images.unsplash.com/photo-1503602642458-232111445657?w=800&q=80", "A mid-century modern masterpiece. Ergonomically designed for maximum comfort and unparalleled style.", "Furniture")
)

val categories = listOf("All", "Shoes", "Accessories", "Electronics", "Furniture")

// --- ViewModel ---
class ShopViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(initialProducts)
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _adminUsername = MutableStateFlow("admin")
    val adminUsername: StateFlow<String> = _adminUsername.asStateFlow()

    private val _adminPassword = MutableStateFlow("password")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    fun addToCart(product: Product) {
        _cartItems.update { currentCart ->
            val existingItem = currentCart.find { it.product.id == product.id }
            if (existingItem != null) {
                currentCart.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                currentCart + CartItem(product, 1)
            }
        }
    }

    fun removeOrDecreaseCartItem(productId: String) {
        _cartItems.update { currentCart ->
            val existingItem = currentCart.find { it.product.id == productId }
            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    currentCart.map { if (it.product.id == productId) it.copy(quantity = it.quantity - 1) else it }
                } else {
                    currentCart.filter { it.product.id != productId }
                }
            } else currentCart
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun getFilteredProducts(): List<Product> {
        val cat = _selectedCategory.value
        return if (cat == "All") _products.value else _products.value.filter { it.category == cat }
    }

    fun addProduct(product: Product) {
        _products.update { it + product }
    }

    fun placeOrder(order: Order) {
        _orders.update { it + order }
    }

    fun createDemoOrder() {
        val demoId = "DEMO-" + (10000..99999).random()
        val demoOrder = Order(
            id = demoId,
            buyerName = "আয়ুশ মন্ডল (Demo Buyer)",
            address = "সেক্টর ৫, উত্তরা, ঢাকা, বাংলাদেশ",
            phone = "01712345678",
            email = "mondalayush264@gmail.com",
            items = listOf(CartItem(initialProducts[0], 1), CartItem(initialProducts[1], 1)),
            total = initialProducts[0].price + initialProducts[1].price,
            status = "Processing"
        )
        _orders.update { it + demoOrder }
    }

    fun updateProductPrice(productId: String, newPrice: Double) {
        _products.update { current ->
            current.map {
                if (it.id == productId) it.copy(price = newPrice) else it
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        _orders.update { current ->
            current.map {
                if (it.id == orderId) it.copy(status = newStatus) else it
            }
        }
    }

    fun updateAdminCredentials(usernameVal: String, passwordVal: String) {
        _adminUsername.value = usernameVal
        _adminPassword.value = passwordVal
    }

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("স্বাগতম! আমি ShopEasy AI অ্যাসিস্ট্যান্ট। আমি আপনাকে সঠিক পণ্য খুঁজে পেতে বা পছন্দমতো সাজেশন্স দিতে সাহায্য করতে পারি। (Welcome! I am your ShopEasy AI Assistant. I can help you find products, compare options, or suggest recommendations.)", false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("স্বাগতম! আমি ShopEasy AI অ্যাসিস্ট্যান্ট। আমি আপনাকে সঠিক পণ্য খুঁজে পেতে বা পছন্দমতো সাজেশন্স দিতে সাহায্য করতে পারি। (Welcome! I am your ShopEasy AI Assistant. I can help you find products, compare options, or suggest recommendations.)", false)
        )
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        
        val currentMessages = _chatMessages.value
        val newMessages = currentMessages + ChatMessage(userText, true)
        _chatMessages.value = newMessages
        _isChatLoading.value = true
        
        viewModelScope.launch {
            val productListContext = _products.value.joinToString(separator = "\n") { 
                "- Category: ${it.category}, Product Name: ${it.name}, Price: ₹${it.price}, ID: ${it.id}, Description: ${it.description}"
            }
            
            val systemInstruction = """
                You are "ShopEasy AI Assistant", a smart and helpful shopping expert for the ShopEasy online store.
                
                Here is the current list of products in our inventory:
                $productListContext
                
                Your Guidelines:
                1. Always prioritize products from the provided inventory list. Identify recommendations, compare options, or answer details based on these products.
                2. If the user asks for a specific price range, list, or category, recommend relevant items from this inventory.
                3. Do not make up products that do not exist here.
                4. Be friendly, helpful, and polite.
                5. Respond in a welcoming mix of Bengali and English (Bilingual or Banglish as appropriate), or copy the language preference of the user.
                6. Advise users that they can browse on the Home screen, click on any product card to see details, click "Add to Cart", and complete their purchase at the Checkout screen!
            """.trimIndent()
            
            val promptBuilder = StringBuilder()
            promptBuilder.append("Conversation History:\n")
            newMessages.takeLast(8).forEach { msg ->
                val role = if (msg.isUser) "User" else "Assistant"
                promptBuilder.append("$role: ${msg.text}\n")
            }
            promptBuilder.append("\nUser: $userText\nAssistant: ")
            
            val reply = queryGemini(promptBuilder.toString(), systemInstruction)
            
            _chatMessages.update { it + ChatMessage(reply, false) }
            _isChatLoading.value = false
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

suspend fun queryGemini(prompt: String, systemInstruction: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Error: Gemini API key is not configured in AI Studio Secrets. Please add GEMINI_API_KEY to AI Studio Secrets."
    }
    
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
    
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
        
    val jsonRequest = org.json.JSONObject().apply {
        val contentsArray = org.json.JSONArray().apply {
            put(org.json.JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("text", prompt)
                    })
                })
            })
        }
        put("contents", contentsArray)
        
        if (systemInstruction.isNotEmpty()) {
            put("systemInstruction", org.json.JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
        }
    }
    
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = jsonRequest.toString().toRequestBody(mediaType)
    
    val request = okhttp3.Request.Builder()
        .url(url)
        .post(body)
        .build()
        
    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext "Error: Unexpected response code ${response.code} - ${response.message}"
            }
            val responseBodyString = response.body?.string() ?: return@withContext "Error: Empty response body"
            val jsonResponse = org.json.JSONObject(responseBodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val content = candidates?.optJSONObject(0)?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")
            
            text ?: "Error: No text in model response."
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

fun copyUriToLocalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "product_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// --- MainActivity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            prefs.edit().putString("CRASH_TRACE", e.stackTraceToString()).commit()
            defaultHandler?.uncaughtException(t, e) ?: System.exit(2)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var crashTrace by remember { mutableStateOf(prefs.getString("CRASH_TRACE", null)) }

                if (crashTrace != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .systemBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Previous Session Information",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "The application encountered an unexpected issue in a previous session. You can dismiss this report to open the app or perform a clean restart.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = crashTrace ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    prefs.edit().clear().apply()
                                    crashTrace = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss & Open App")
                            }

                            OutlinedButton(
                                onClick = {
                                    prefs.edit().clear().apply()
                                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                                    if (intent != null) {
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        startActivity(intent)
                                    } else {
                                        android.os.Process.killProcess(android.os.Process.myPid())
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear & Restart")
                            }
                        }
                    }
                } else {
                    ShopApp()
                }
            }
        }
    }
}

// --- App Entry Point ---
@Composable
fun ShopApp() {
    val navController = rememberNavController()
    val viewModel: ShopViewModel = viewModel()
    
    // We will use a standard scaffold for global structure
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController, viewModel)
        }
        composable(
            "detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val product = viewModel.products.collectAsState().value.find { it.id == productId }
            if (product != null) {
                DetailScreen(product, navController, viewModel)
            }
        }
        composable("cart") {
            CartScreen(navController, viewModel)
        }
        composable("checkout") {
            CheckoutScreen(navController, viewModel)
        }
        composable("admin") {
            AdminScreen(navController, viewModel)
        }
        composable("tracking") {
            TrackingScreen(navController, viewModel)
        }
        composable("ai_assistant") {
            AIAssistantScreen(navController, viewModel)
        }
    }
}

// --- Screens ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, viewModel: ShopViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val products = if (selectedCategory == "All") allProducts else allProducts.filter { it.category == selectedCategory }
    val cart by viewModel.cartItems.collectAsState()
    val cartCount = cart.sumOf { it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_shopeasy_logo_1780310943705),
                                contentDescription = "ShopEasy Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text("ShopEasy", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("ai_assistant") }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Assistant", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { navController.navigate("tracking") }) {
                        Icon(Icons.Filled.List, contentDescription = "My Orders")
                    }
                    IconButton(onClick = { navController.navigate("admin") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Admin Panel")
                    }
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart")
                        }
                        if (cartCount > 0) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(
                                    text = "$cartCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // AI Assistant Greeting Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { navController.navigate("ai_assistant") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "স্মার্ট শপ অ্যাসিস্ট্যান্ট (Shop AI)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "পণ্য খোঁজার গাইড এবং সাজেশনের জন্য AI এর সাথে চ্যাট করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        "চ্যাট করুন →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Delivery Tracking Greeting Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { navController.navigate("tracking") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = "Delivery Tracking",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ডেলিভারি ট্র্যাকিং (Delivery Tracking)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "আপনার অর্ডারের লাইভ ডেলিভারি স্ট্যাটাস ও প্রোগ্রেস চেক করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        "ট্র্যাক করুন →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Products Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products) { product ->
                    ProductCard(product) {
                        navController.navigate("detail/${product.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${product.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(product: Product, navController: NavHostController, viewModel: ShopViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Price", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${product.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = { 
                            viewModel.addToCart(product) 
                            android.widget.Toast.makeText(context, "${product.name} added to cart", android.widget.Toast.LENGTH_SHORT).show()
                            navController.navigate("cart")
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Cart")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            )
            
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = product.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.sendChatMessage("আমাকে ${product.name} সম্পর্কে আরও বলুন এবং এটি কেনার সুবিধা কী? (Tell me more about ${product.name} and why I should buy it?)")
                            navController.navigate("ai_assistant")
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Expert",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ask our Shop AI Expert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Get instant AI insights on ${product.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            "→",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavHostController, viewModel: ShopViewModel) {
    val cart by viewModel.cartItems.collectAsState()
    val total = cart.sumOf { it.product.price * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (cart.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "₹${(total * 100).toInt() / 100.0}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate("checkout") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Proceed to Checkout", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your cart is empty", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cart) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { viewModel.addToCart(item.product) },
                        onDecrease = { viewModel.removeOrDecreaseCartItem(item.product.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${item.product.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onIncrease) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

// --- New Screens ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(navController: NavHostController, viewModel: ShopViewModel) {
    var name by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var address by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var phone by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var email by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var orderPlaced by remember { mutableStateOf(false) }
    var createdOrderId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (orderPlaced) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "অর্ডার সফলভাবে সম্পন্ন হয়েছে!", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ধন্যবাদ! নিচে দেওয়া আইডি দিয়ে আপনার ডেলিভারি ট্র্যাকিং করতে পারবেন:",
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#${createdOrderId.take(8).uppercase()}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.clearCart()
                                navController.popBackStack("home", false) 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("হোমে ফিরে যান")
                        }
                        Button(
                            onClick = { 
                                viewModel.clearCart()
                                navController.navigate("tracking") {
                                    popUpTo("home") { saveState = true }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ডেলিভারি ট্র্যাক করুন")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Buyer Information", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Delivery Address") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(
                    value = phone, 
                    onValueChange = { phone = it }, 
                    label = { Text("Phone Number") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("Email Address") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        val cart = viewModel.cartItems.value
                        val total = cart.sumOf { it.product.price * it.quantity }
                        val orderId = java.util.UUID.randomUUID().toString()
                        createdOrderId = orderId
                        val order = Order(
                            id = orderId,
                            buyerName = name.text,
                            address = address.text,
                            phone = phone.text,
                            email = email.text,
                            items = cart,
                            total = total
                        )
                        viewModel.placeOrder(order)
                        orderPlaced = true 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = name.text.isNotBlank() && address.text.isNotBlank() && phone.text.isNotBlank()
                ) {
                    Text("Place Order")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavHostController, viewModel: ShopViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val products by viewModel.products.collectAsState()
    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var password by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var loginError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!isLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Admin Login", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; loginError = false },
                    label = { Text("User ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        autoCorrectEnabled = false,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; loginError = false },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                if (loginError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Invalid credentials", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val expectedUser = viewModel.adminUsername.value
                        val expectedPass = viewModel.adminPassword.value
                        if (username.text.trim().lowercase() == expectedUser.lowercase() && password.text.trim() == expectedPass) {
                            isLoggedIn = true
                        } else {
                            loginError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Login")
                }
            }
        } else {
            var selectedTab by remember { mutableStateOf(0) }
            val tabTitles = listOf("Products", "Add", "Orders", "Credentials")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text("Manage Products", style = MaterialTheme.typography.titleMedium)
                                Text("Update prices for items below:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(products) { product ->
                                var priceText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(product.price.toString())) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                                            Text("Current: ₹${product.price}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        OutlinedTextField(
                                            value = priceText,
                                            onValueChange = { priceText = it },
                                            modifier = Modifier.width(100.dp),
                                            singleLine = true,
                                            label = { Text("New Price") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                        Button(onClick = { 
                                            priceText.text.toDoubleOrNull()?.let { newPrice ->
                                                viewModel.updateProductPrice(product.id, newPrice)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Price updated",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }) {
                                            Text("Update")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        var newName by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
                        var newPrice by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
                        var newImage by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
                        var newDesc by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
                        var newCategory by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("All")) }
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            uri?.let {
                                val localPath = copyUriToLocalStorage(context, it)
                                if (localPath != null) {
                                    newImage = androidx.compose.ui.text.input.TextFieldValue("file://$localPath")
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Add New Product", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(
                                value = newPrice, 
                                onValueChange = { newPrice = it }, 
                                label = { Text("Price (e.g., 500)") }, 
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            
                            Text("Product Image (ছবি বাছুন)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (newImage.text.isNotBlank()) {
                                    AsyncImage(
                                        model = newImage.text,
                                        contentDescription = "New Product Image Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = "Change Image",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text("ছবি পরিবর্তন করুন (Change Image)", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Image,
                                            contentDescription = "Upload Image",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text("গ্যালারি থেকে ছবি আপলোড করুন", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("Choose from Gallery", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("অথবা ডেমো ছবি বাছুন (Or choose a demo image):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presets = listOf(
                                        "Shoes" to "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80",
                                        "Watch" to "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80",
                                        "Electronics" to "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80",
                                        "Furniture" to "https://images.unsplash.com/photo-1503602642458-232111445657?w=800&q=80",
                                        "Clothes" to "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&q=80",
                                        "Grocery" to "https://images.unsplash.com/photo-1610832958506-ee5633619144?w=800&q=80"
                                    )
                                    presets.forEach { (label, url) ->
                                        val isSelected = newImage.text == url
                                        SuggestionChip(
                                            onClick = { newImage = androidx.compose.ui.text.input.TextFieldValue(url) },
                                            label = { Text(label) },
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                        )
                                    }
                                }
                            }

                            var showUrlInput by remember { mutableStateOf(false) }
                            TextButton(onClick = { showUrlInput = !showUrlInput }) {
                                Text(if (showUrlInput) "ম্যানুয়াল URL ইনপুট বন্ধ করুন" else "ম্যানুয়ালি ইমেজ URL দিন (Advanced)", style = MaterialTheme.typography.labelMedium)
                            }
                            if (showUrlInput) {
                                OutlinedTextField(
                                    value = newImage,
                                    onValueChange = { newImage = it },
                                    label = { Text("Image URL") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    val price = newPrice.text.toDoubleOrNull()
                                    if (newName.text.isBlank()) {
                                        android.widget.Toast.makeText(context, "দয়া করে প্রোডাক্টের নাম দিন (Please enter product name)", android.widget.Toast.LENGTH_SHORT).show()
                                    } else if (price == null) {
                                        android.widget.Toast.makeText(context, "দয়া করে সঠিক মূল্য লিখুন (Please enter a valid price)", android.widget.Toast.LENGTH_SHORT).show()
                                    } else if (newImage.text.isBlank()) {
                                        android.widget.Toast.makeText(context, "দয়া করে একটি ছবি সিলেক্ট করুন (Please select an image)", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addProduct(Product(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = newName.text,
                                            price = price,
                                            imageUrl = newImage.text,
                                            description = newDesc.text,
                                            category = newCategory.text
                                        ))
                                        newName = androidx.compose.ui.text.input.TextFieldValue("")
                                        newPrice = androidx.compose.ui.text.input.TextFieldValue("")
                                        newImage = androidx.compose.ui.text.input.TextFieldValue("")
                                        newDesc = androidx.compose.ui.text.input.TextFieldValue("")
                                        newCategory = androidx.compose.ui.text.input.TextFieldValue("All")
                                        android.widget.Toast.makeText(context, "প্রোডাক্ট সফলভাবে যোগ করা হয়েছে (Product Added successfully)", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save Product") }
                        }
                    }
                    2 -> {
                        val orders = viewModel.orders.collectAsState().value
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text("Recent Orders & Delivery Tracking", style = MaterialTheme.typography.titleMedium)
                                Text("Select a tracking status below to update the client's order progress:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            items(orders) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Order ID: #${order.id.take(8).uppercase()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Order total: ₹${order.total}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        Text("Buyer: ${order.buyerName} - ${order.phone}", style = MaterialTheme.typography.bodySmall)
                                        Text("Address: ${order.address}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Items:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        order.items.forEach { item ->
                                            Text("- ${item.quantity}x ${item.product.name}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Current Status: ${order.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val statuses = listOf("Pending", "Processing", "Shipped", "Delivered")
                                            statuses.forEach { s ->
                                                val isSelected = order.status == s
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { viewModel.updateOrderStatus(order.id, s) },
                                                    label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        var editUsername by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(viewModel.adminUsername.value)) }
                        var editPassword by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(viewModel.adminPassword.value)) }
                        var changeSuccess by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Change Admin Credentials", style = MaterialTheme.typography.titleMedium)
                            Text("Update the username and password required to log into the Admin Panel.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it; changeSuccess = false },
                                label = { Text("New Admin User ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = editPassword,
                                onValueChange = { editPassword = it; changeSuccess = false },
                                label = { Text("New Admin Password") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            AnimatedVisibility(visible = changeSuccess) {
                                Text("Credentials updated successfully!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Button(
                                onClick = {
                                    if (editUsername.text.isNotBlank() && editPassword.text.isNotBlank()) {
                                        viewModel.updateAdminCredentials(editUsername.text.trim(), editPassword.text.trim())
                                        changeSuccess = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = editUsername.text.isNotBlank() && editPassword.text.isNotBlank()
                            ) {
                                Text("Save Credentials")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Firebase Integration Details", style = MaterialTheme.typography.titleMedium)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = "Firebase Connected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "ফায়ারবেস সফলভাবে যুক্ত হয়েছে! (Firebase Connected)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Project ID: meghna-online\nProject Number: 481409571137\nStatus: Google Services Plugin Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(navController: NavHostController, viewModel: ShopViewModel) {
    val orders by viewModel.orders.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrderForTracking by remember { mutableStateOf<Order?>(null) }
    
    // Automatically select the latest order as default if none selected and orders list shifts
    LaunchedEffect(orders) {
        if (selectedOrderForTracking == null && orders.isNotEmpty()) {
            selectedOrderForTracking = orders.last()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ডেলিভারি ট্র্যাকিং ও অর্ডারসমূহ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick search bar at top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "অর্ডার আইডি দিয়ে ট্র্যাকিং করুন:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            val trimmed = query.trim().uppercase().removePrefix("#")
                            if (trimmed.isNotEmpty()) {
                                val found = orders.find { it.id.uppercase().contains(trimmed) || it.id.uppercase().take(8).contains(trimmed) }
                                if (found != null) {
                                    selectedOrderForTracking = found
                                }
                            }
                        },
                        placeholder = { Text("যেমন: DEMO বা অর্ডারের সংক্ষিপ্ত ID") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = "" 
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "কোনো অর্ডার পাওয়া যায়নি।", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "একটি পণ্য অর্ডার করুন অথবা নিচে বাটনে ক্লিক করে একটি ডেমো অর্ডার তৈরি করুন ট্র্যাকিং দেখার জন্য!", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { viewModel.createDemoOrder() }) {
                                Text("ডেমো অর্ডার তৈরি করুন")
                            }
                            OutlinedButton(onClick = { 
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }) {
                                Text("শপিং করতে যান")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Live Selected tracking details
                    selectedOrderForTracking?.let { order ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.LocalShipping,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "লাইভ ট্র্যাকার: #${order.id.take(8).uppercase()}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "ক্রেতা: ${order.buyerName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                        
                                        Surface(
                                            color = when (order.status) {
                                                "Pending" -> Color(0xFFFFF3CD)
                                                "Processing" -> Color(0xFFD1ECF1)
                                                "Shipped" -> Color(0xFFCCE5FF)
                                                "Delivered" -> Color(0xFFD4EDDA)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            contentColor = when (order.status) {
                                                "Pending" -> Color(0xFF856404)
                                                "Processing" -> Color(0xFF0C5460)
                                                "Shipped" -> Color(0xFF004085)
                                                "Delivered" -> Color(0xFF155724)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = order.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Tracking Carrier details
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "শিপিং পার্টনার: ShopEasy Live Express",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "সম্ভাব্য সময়: ২-৩ কার্যদিবস",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Live Stepper for delivery status progress
                                    OrderTracker(order.status)
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Items details
                                    Text("অর্ডার করা পণ্যসমূহ (${order.items.size}):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    order.items.forEach { item ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            AsyncImage(
                                                model = item.product.imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${item.quantity}x ${item.product.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "₹${item.product.price * item.quantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ঠিকানা: ${order.address}", 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = Color.Gray, 
                                            maxLines = 1, 
                                            overflow = TextOverflow.Ellipsis, 
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "মোট: ₹${order.total}", 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Bold, 
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 2. Order History Header
                    item {
                        Text(
                            text = "আপনার সকল অর্ডারসমূহ (${orders.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // 3. Historical Order Cards
                    items(orders.reversed()) { order ->
                        val isSelected = selectedOrderForTracking?.id == order.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOrderForTracking = order },
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "অর্ডার আইডি: #${order.id.take(8).uppercase()}", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${order.items.size}টি পণ্য • ₹${order.total}", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "ঠিকানা: ${order.address}", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = Color.Gray, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = when (order.status) {
                                            "Pending" -> Color(0xFFFFF3CD)
                                            "Processing" -> Color(0xFFD1ECF1)
                                            "Shipped" -> Color(0xFFCCE5FF)
                                            "Delivered" -> Color(0xFFD4EDDA)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = when (order.status) {
                                            "Pending" -> Color(0xFF856404)
                                            "Processing" -> Color(0xFF0C5460)
                                            "Shipped" -> Color(0xFF004085)
                                            "Delivered" -> Color(0xFF155724)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = order.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ট্র্যাক করুন →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTracker(currentStatus: String) {
    val stages = listOf("Pending", "Processing", "Shipped", "Delivered")
    val stageLabelsBengali = listOf("অর্ডার গ্রহণ", "প্রক্রিয়াধীন", "পাঠানো হয়েছে", "ডেলিভারি সম্পন্ন")
    val currentIndex = stages.indexOf(currentStatus).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("ডেলিভারি ট্র্যাকিং (Delivery Tracking)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        stages.forEachIndexed { index, stage ->
            val isCurrent = index == currentIndex
            val isPassed = index <= currentIndex
            val labelColor = if (isPassed) MaterialTheme.colorScheme.primary else Color.Gray
            val circleColor = if (isCurrent) MaterialTheme.colorScheme.primary else if (isPassed) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.LightGray

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(circleColor, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed && !isCurrent) {
                        Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = stage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = labelColor
                    )
                    Text(
                        text = stageLabelsBengali[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor.copy(alpha = 0.8f)
                    )
                }
            }
            
            if (index < stages.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .width(2.dp)
                        .height(20.dp)
                        .background(if (index < currentIndex) MaterialTheme.colorScheme.primary else Color.LightGray)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(navController: NavHostController, viewModel: ShopViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var userText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shop AI Assistant")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clearChat() }) {
                        Text("Clear Chat")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userText,
                        onValueChange = { userText = it },
                        placeholder = { Text("এখানে লিখুন... (Type here...)") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (userText.text.isNotBlank() && !isChatLoading) {
                                IconButton(onClick = {
                                    val txt = userText.text
                                    userText = androidx.compose.ui.text.input.TextFieldValue("")
                                    viewModel.sendChatMessage(txt)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                    IconButton(
                        onClick = {
                            val txt = userText.text
                            if (txt.isNotBlank()) {
                                userText = androidx.compose.ui.text.input.TextFieldValue("")
                                viewModel.sendChatMessage(txt)
                            }
                        },
                        enabled = userText.text.isNotBlank() && !isChatLoading,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "সবচেয়ে সস্তা জুতো কোনটি? (Cheapest shoes?)",
                    "Sony Headphone সম্পর্কে বলুন",
                    "সেরা ঘড়ি কোনটা হবে? (Best Watch?)",
                    "সব পণ্য দেখান (Show all products)"
                )
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = {
                            viewModel.sendChatMessage(suggestion)
                        },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages) { message ->
                    ChatBubble(message)
                }
                if (isChatLoading) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 12.dp).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("ShopEasy AI টাইপ করছে...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
