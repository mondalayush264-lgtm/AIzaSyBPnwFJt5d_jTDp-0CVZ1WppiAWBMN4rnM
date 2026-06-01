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
                title = { Text("ShopEasy", style = MaterialTheme.typography.titleLarge) },
                actions = {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Order Placed Successfully!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        viewModel.clearCart()
                        navController.popBackStack("home", false) 
                    }) {
                        Text("Back to Home")
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
                        val order = Order(
                            id = java.util.UUID.randomUUID().toString(),
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
                            OutlinedTextField(value = newImage, onValueChange = { newImage = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth())
                            if (newImage.text.isNotBlank()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = newImage.text,
                                            contentDescription = "New Product Image Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                        ) {
                                            Text(
                                                "Image Preview",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    val price = newPrice.text.toDoubleOrNull()
                                    if (newName.text.isNotBlank() && price != null && newImage.text.isNotBlank()) {
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
                                        android.widget.Toast.makeText(context, "Product Added", android.widget.Toast.LENGTH_SHORT).show()
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("অর্ডার সমূহ ও ট্র্যাকিং (My Orders)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("কোনো অর্ডার পাওয়া যায়নি।", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("আগে একটি পণ্য অর্ডার করুন!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.navigate("home") }) {
                        Text("শপিং করতে ফিরে যান")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders.reversed()) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("অর্ডার আইডি: #${order.id.take(8).uppercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("টোটাল পেমেন্ট: ₹${order.total}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
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
                            
                            Text("অর্ডার করা পণ্যসমূহ:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
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
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            OrderTracker(order.status)
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
