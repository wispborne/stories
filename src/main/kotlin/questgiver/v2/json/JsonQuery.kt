package wisp.questgiver.v2.json

import org.json.JSONObject

/**
 * Creates a JSONPointer using an initialization string and tries to
 * match it to an item within this JSONObject. For example, given a
 * JSONObject initialized with this document:
 * <pre>
 * {
 * "a":{"b":"c"}
 * }
</pre> *
 * and this JSONPointer string:
 * <pre>
 * "/a/b"
</pre> *
 * Then this method will return the String "c".
 * A JSONPointerException may be thrown from code called by this method.
 *
 * @param jsonPointer string that can be used to create a JSONPointer
 * @return the item matched by the JSONPointer, otherwise null
 */
fun <T> JSONObject.query(jsonPointer: String?): T {
    return query(JSONPointer(jsonPointer)) as T
}

/**
 * Uses a user initialized JSONPointer  and tries to
 * match it to an item within this JSONObject. For example, given a
 * JSONObject initialized with this document:
 * <pre>
 * {
 * "a":{"b":"c"}
 * }
</pre> *
 * and this JSONPointer:
 * <pre>
 * "/a/b"
</pre> *
 * Then this method will return the String "c".
 * A JSONPointerException may be thrown from code called by this method.
 *
 * @param jsonPointer string that can be used to create a JSONPointer
 * @return the item matched by the JSONPointer, otherwise null
 */
fun <T> JSONObject.query(jsonPointer: JSONPointer): T {
    return jsonPointer.queryFrom(this) as T
}

/**
 * Queries and returns a value from this object using `jsonPointer`, or
 * returns null if the query fails due to a missing key.
 *
 * @param jsonPointer the string representation of the JSON pointer
 * @return the queried value or `null`
 * @throws IllegalArgumentException if `jsonPointer` has invalid syntax
 */
fun <T> JSONObject.optQuery(jsonPointer: String?): T? {
    return optQuery(JSONPointer(jsonPointer)) as? T
}

/**
 * Queries and returns a value from this object using `jsonPointer`, or
 * returns null if the query fails due to a missing key.
 *
 * @param jsonPointer The JSON pointer
 * @return the queried value or `null`
 * @throws IllegalArgumentException if `jsonPointer` has invalid syntax
 */
fun <T> JSONObject.optQuery(jsonPointer: JSONPointer): T? {
    return try {
        jsonPointer.queryFrom(this) as? T
    } catch (e: JSONPointerException) {
        null
    }
}