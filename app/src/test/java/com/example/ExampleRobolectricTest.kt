package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testFullCustomerToAdminE2EFlow() {
        try {
            // Verify we are on Home page
            composeTestRule.onNodeWithText("ShopEasy").assertExists()

            // 1. Click on the first product (Nike Air Max)
            composeTestRule.onNodeWithText("Nike Air Max").performClick()
            
            // Inside detail screen
            composeTestRule.onNodeWithText("Description").assertExists()

            // 2. Add to Cart (this adds and navigates to the Cart screen)
            composeTestRule.onNodeWithText("Add to Cart").performClick()
            
            // Inside Cart Screen
            composeTestRule.onNodeWithText("Your Cart").assertExists()
            composeTestRule.onNodeWithText("Nike Air Max").assertExists()
            
            // We have two nodes containing ₹4500.0 (the unit price and the total price)
            composeTestRule.onAllNodesWithText("₹4500.0").onFirst().assertExists()

            // 3. Increase quantity by clicking "+"
            composeTestRule.onNodeWithText("+").performClick()
            composeTestRule.onNodeWithText("2").assertExists() // quantity is now 2
            composeTestRule.onNodeWithText("Total").assertExists()

            // 4. Click Proceed to Checkout
            composeTestRule.onNodeWithText("Proceed to Checkout").performClick()

            // Inside Checkout Screen
            composeTestRule.onNodeWithText("Buyer Information").assertExists()
            
            // Enter details
            composeTestRule.onNodeWithText("Full Name").performTextReplacement("Test User")
            composeTestRule.onNodeWithText("Delivery Address").performTextReplacement("123 Main St, Dhaka")
            composeTestRule.onNodeWithText("Phone Number").performTextReplacement("01712345678")
            composeTestRule.onNodeWithText("Email Address").performTextReplacement("test@example.com")

            // Scroll to button first to ensure visibility on all simulated screen sizes
            composeTestRule.onNodeWithText("Place Order").performScrollTo().performClick()
            composeTestRule.onNodeWithText("Order Placed Successfully!").assertExists()

            // 5. Click Back to Home
            composeTestRule.onNodeWithText("Back to Home").performClick()
            composeTestRule.onNodeWithText("ShopEasy").assertExists()

            // 6. View My Orders / Tracking Screen
            composeTestRule.onNodeWithContentDescription("My Orders").performClick()
            composeTestRule.onNodeWithText("অর্ডার সমূহ ও ট্র্যাকিং (My Orders)").assertExists()
            
            // Go back to Home
            composeTestRule.onNodeWithContentDescription("Back").performClick()

            // 7. Login to Admin to change order status
            composeTestRule.onNodeWithContentDescription("Admin Panel").performClick()
            composeTestRule.onNodeWithText("User ID").performTextReplacement("admin")
            composeTestRule.onNodeWithText("Password").performTextReplacement("password")
            composeTestRule.onNodeWithText("Login").performClick()

            // Switch to Orders Tab
            composeTestRule.onNodeWithText("Orders").performClick()

            // Update status to Shipped
            composeTestRule.onNodeWithText("Shipped").performClick()
            
            // Back to Home
            composeTestRule.onNodeWithContentDescription("Back").performClick()

            // 8. Re-open Tracking Screen to verify updated status
            composeTestRule.onNodeWithContentDescription("My Orders").performClick()
            composeTestRule.onNodeWithText("Shipped").assertExists()
        } catch (t: Throwable) {
            println("DEBUGLOG: FAILED WITH EXCEPTION")
            t.printStackTrace()
            throw t
        }
    }
}
