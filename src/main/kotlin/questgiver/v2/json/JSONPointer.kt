package wisp.questgiver.v2.json

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
/**
 * A JSON Pointer is a simple query language defined for JSON documents by
 * [RFC 6901](https://tools.ietf.org/html/rfc6901).
 *
 * In a nutshell, JSONPointer allows the user to navigate into a JSON document
 * using strings, and retrieve targeted objects, like a simple form of XPATH.
 * Path segments are separated by the '/' char, which signifies the root of
 * the document when it appears as the first char of the string. Array
 * elements are navigated using ordinals, counting from 0. JSONPointer strings
 * may be extended to any arbitrary number of segments. If the navigation
 * is successful, the matched item is returned. A matched item may be a
 * JSONObject, a JSONArray, or a JSON value. If the JSONPointer string building
 * fails, an appropriate exception is thrown. If the navigation fails to find
 * a match, a JSONPointerException is thrown.
 *
 * @author JSON.org
 * @version 2016-05-14
 */
class JSONPointer {
    /**
     * This class allows the user to build a JSONPointer in steps, using
     * exactly one segment in each step.
     */
    class Builder {
        // Segments for the eventual JSONPointer string
        private val refTokens: MutableList<String> = ArrayList()

        /**
         * Creates a `JSONPointer` instance using the tokens previously set using the
         * [.append] method calls.
         * @return a JSONPointer object
         */
        fun build(): JSONPointer {
            return JSONPointer(refTokens)
        }

        /**
         * Adds an arbitrary token to the list of reference tokens. It can be any non-null value.
         *
         * Unlike in the case of JSON string or URI fragment representation of JSON pointers, the
         * argument of this method MUST NOT be escaped. If you want to query the property called
         * `"a~b"` then you should simply pass the `"a~b"` string as-is, there is no
         * need to escape it as `"a~0b"`.
         *
         * @param token the new token to be appended to the list
         * @return `this`
         * @throws NullPointerException if `token` is null
         */
        fun append(token: String?): Builder {
            if (token == null) {
                throw NullPointerException("token cannot be null")
            }
            refTokens.add(token)
            return this
        }

        /**
         * Adds an integer to the reference token list. Although not necessarily, mostly this token will
         * denote an array index.
         *
         * @param arrayIndex the array index to be added to the token list
         * @return `this`
         */
        fun append(arrayIndex: Int): Builder {
            refTokens.add(arrayIndex.toString())
            return this
        }
    }

    // Segments for the JSONPointer string
    private val refTokens: List<String>

    /**
     * Pre-parses and initializes a new `JSONPointer` instance. If you want to
     * evaluate the same JSON Pointer on different JSON documents then it is recommended
     * to keep the `JSONPointer` instances due to performance considerations.
     *
     * @param pointer the JSON String or URI Fragment representation of the JSON pointer.
     * @throws IllegalArgumentException if `pointer` is not a valid JSON pointer
     */
    constructor(pointer: String?) {
        if (pointer == null) {
            throw NullPointerException("pointer cannot be null")
        }
        if (pointer.isEmpty() || pointer == "#") {
            refTokens = emptyList()
            return
        }
        var refs: String
        if (pointer.startsWith("#/")) {
            refs = pointer.substring(2)
            refs = try {
                URLDecoder.decode(refs, ENCODING)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        } else if (pointer.startsWith("/")) {
            refs = pointer.substring(1)
        } else {
            throw IllegalArgumentException("a JSON pointer should start with '/' or '#/'")
        }
        refTokens = ArrayList()
        var slashIdx = -1
        var prevSlashIdx = 0
        do {
            prevSlashIdx = slashIdx + 1
            slashIdx = refs.indexOf('/', prevSlashIdx)
            if (prevSlashIdx == slashIdx || prevSlashIdx == refs.length) {
                // found 2 slashes in a row ( obj//next )
                // or single slash at the end of a string ( obj/test/ )
                refTokens.add("")
            } else if (slashIdx >= 0) {
                val token = refs.substring(prevSlashIdx, slashIdx)
                refTokens.add(unescape(token))
            } else {
                // last item after separator, or no separator at all.
                val token = refs.substring(prevSlashIdx)
                refTokens.add(unescape(token))
            }
        } while (slashIdx >= 0)
        // using split does not take into account consecutive separators or "ending nulls"
        //for (String token : refs.split("/")) {
        //    this.refTokens.add(unescape(token));
        //}
    }

    constructor(refTokens: List<String>?) {
        this.refTokens = ArrayList(refTokens)
    }

    /**
     * Evaluates this JSON Pointer on the given `document`. The `document`
     * is usually a [JSONObject] or a [JSONArray] instance, but the empty
     * JSON Pointer (`""`) can be evaluated on any JSON values and in such case the
     * returned value will be `document` itself.
     *
     * @param document the JSON document which should be the subject of querying.
     * @return the result of the evaluation
     * @throws JSONPointerException if an error occurs during evaluation
     */
    @Throws(JSONPointerException::class)
    fun queryFrom(document: Any): Any {
        if (refTokens.isEmpty()) {
            return document
        }
        var current = document
        for (token in refTokens) {
            current = if (current is JSONObject) {
                current.opt(unescape(token))
            } else if (current is JSONArray) {
                readByIndexToken(current, token)
            } else {
                throw JSONPointerException(
                    String.format(
                        "value [%s] is not an array or object therefore its key %s cannot be resolved", current,
                        token
                    )
                )
            }
        }
        return current
    }

    /**
     * Returns a string representing the JSONPointer path value using string
     * representation
     */
    override fun toString(): String {
        val rval = StringBuilder("")
        for (token in refTokens) {
            rval.append('/').append(escape(token))
        }
        return rval.toString()
    }

    /**
     * Returns a string representing the JSONPointer path value using URI
     * fragment identifier representation
     * @return a uri fragment string
     */
    fun toURIFragment(): String {
        return try {
            val rval = StringBuilder("#")
            for (token in refTokens) {
                rval.append('/').append(URLEncoder.encode(token, ENCODING))
            }
            rval.toString()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        // used for URL encoding and decoding
        private const val ENCODING = "utf-8"

        /**
         * Static factory method for [Builder]. Example usage:
         *
         * <pre>`
         * JSONPointer pointer = JSONPointer.builder()
         * .append("obj")
         * .append("other~key").append("another/key")
         * .append("\"")
         * .append(0)
         * .build();
        `</pre> *
         *
         * @return a builder instance which can be used to construct a `JSONPointer` instance by chained
         * [Builder.append] calls.
         */
        fun builder(): Builder {
            return Builder()
        }

        /**
         * @see [rfc6901 section 3](https://tools.ietf.org/html/rfc6901.section-3)
         */
        private fun unescape(token: String): String {
            return token.replace("~1", "/").replace("~0", "~")
        }

        /**
         * Matches a JSONArray element by ordinal position
         * @param current the JSONArray to be evaluated
         * @param indexToken the array index in string form
         * @return the matched object. If no matching item is found a
         * @throws JSONPointerException is thrown if the index is out of bounds
         */
        @Throws(JSONPointerException::class)
        private fun readByIndexToken(current: Any, indexToken: String): Any {
            return try {
                val index = indexToken.toInt()
                val currentArr = current as JSONArray
                if (index >= currentArr.length()) {
                    throw JSONPointerException(
                        String.format(
                            "index %s is out of bounds - the array has %d elements", indexToken,
                            Integer.valueOf(currentArr.length())
                        )
                    )
                }
                try {
                    currentArr[index]
                } catch (e: JSONException) {
                    throw JSONPointerException("Error reading value at index position $index", e)
                }
            } catch (e: NumberFormatException) {
                throw JSONPointerException(String.format("%s is not an array index", indexToken), e)
            }
        }

        /**
         * Escapes path segment values to an unambiguous form.
         * The escape char to be inserted is '~'. The chars to be escaped
         * are ~, which maps to ~0, and /, which maps to ~1.
         * @param token the JSONPointer segment value to be escaped
         * @return the escaped value for the token
         *
         * @see [rfc6901 section 3](https://tools.ietf.org/html/rfc6901.section-3)
         */
        private fun escape(token: String): String {
            return token.replace("~", "~0")
                .replace("/", "~1")
        }
    }
}