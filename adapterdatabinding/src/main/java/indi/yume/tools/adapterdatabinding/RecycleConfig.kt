package indi.yume.tools.adapterdatabinding

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(flag = true, value = [DO_NOTHING, CLEAR_ITEM, CLEAR_COLLECTION, CLEAR_HANDLERS, CLEAR_ALL])
annotation class RecycleConfig

/**
 * When the [RecyclerView] recycles a view, do nothing. This is the default behavior.
 */
const val DO_NOTHING = 0

/**
 * When the [RecyclerView] recycles a view, reset the item from the [Supplier]
 * to `null`.
 */
const val CLEAR_ITEM = 1

/**
 * When the [RecyclerView] recycles a view, reset and all handlers to `null`.
 */
const val CLEAR_HANDLERS = 1 shl 1

/**
 * When the [RecyclerView] recycles a view, reset the collection from the
 * [Supplier] to `null`.
 */
const val CLEAR_COLLECTION = 1 shl 2

/**
 * When the [RecyclerView] recycles a view, rebind all variables to `null`.
 */
const val CLEAR_ALL = CLEAR_ITEM or CLEAR_COLLECTION or CLEAR_HANDLERS
