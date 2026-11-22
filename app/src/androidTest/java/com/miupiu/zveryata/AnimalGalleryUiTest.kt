package com.miupiu.zveryata

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test

class AnimalGalleryUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitFirstPhoto() {
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithTag("animalImage", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun showsFirstAnimalAndSeparatelyRenderedCaption() {
        awaitFirstPhoto()
        rule.onNodeWithTag("captionHeader", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("photoRegion", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("animalLabel", useUnmergedTree = true).assertTextEquals("Ари")
    }

    @Test fun tapsDoNotNavigate() {
        awaitFirstPhoto()
        rule.onNodeWithTag("animalGallery").performTouchInput { click() }
        rule.onNodeWithTag("animalLabel", useUnmergedTree = true).assertTextEquals("Ари")
    }

    @Test fun swipeLeftShowsNextAnimal() {
        awaitFirstPhoto()
        rule.onNodeWithTag("animalGallery").performTouchInput { swipeLeft() }
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("котёнок", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("animalLabel", useUnmergedTree = true).assertTextEquals("котёнок")
    }

    @Test fun swipeUpShowsNextAnimal() {
        awaitFirstPhoto()
        rule.onNodeWithTag("animalGallery").performTouchInput { swipeUp() }
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("котёнок", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("animalLabel", useUnmergedTree = true).assertTextEquals("котёнок")
    }
}
