package de.werner.cookmore

import kotlinx.serialization.Serializable

@Serializable
class Recipe(var path: String, var url: String, var title: String, var id: Int, var image_url: String) {

    override fun equals(other: Any?): Boolean {
        if (other is Recipe) {
            return id == other.id
        }
        return false
    }
}
