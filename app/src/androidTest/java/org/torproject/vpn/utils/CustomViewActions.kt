package org.torproject.vpn.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.StringDescription
import org.hamcrest.TypeSafeMatcher
import org.torproject.vpn.ui.exitselection.data.ExitNodeAdapter
import java.lang.String.format
import java.util.*
import kotlin.collections.ArrayList


object CustomViewActions {
    fun actionOnItemView(matcher: Matcher<View?>, action: ViewAction): ViewAction {
        return object : ViewAction {

            override fun getDescription(): String {
                return format(
                    "performing ViewAction: %s on item matching: %s",
                    action.description,
                    StringDescription.asString(matcher))
            }

            override fun getConstraints(): Matcher<View> {
                return allOf(withParent(isAssignableFrom(RecyclerView::class.java)), isDisplayed())
            }

            override fun perform(uiController: UiController?, view: View?) {
                val results: MutableList<View> = ArrayList()
                for (v in TreeIterables.breadthFirstViewTraversal(view)) {
                    if (matcher.matches(v)) results.add(v)
                }
                if (results.isEmpty()) {
                    throw RuntimeException(
                        format(
                            "No view found %s",
                            StringDescription.asString(matcher)
                        )
                    )
                } else if (results.size > 1) {
                    throw RuntimeException(
                        format(
                            "Ambiguous views found %s",
                            StringDescription.asString(matcher)
                        )
                    )
                }
                action.perform(uiController, results[0])
            }
        }
    }



}