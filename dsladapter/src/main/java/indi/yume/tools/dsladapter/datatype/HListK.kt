package indi.yume.tools.dsladapter.datatype

import arrow.Kind
import indi.yume.tools.dsladapter.ForIdT
import indi.yume.tools.dsladapter.IdT
import java.util.*

/**
 * An Heterogeneous list of values that preserves type information
 *
 * LIFO (Last In, First out)
 */
sealed class HListK<F, A: HListK<F, A>> {
    // Add item at top.
    abstract fun <T> extend(e: Kind<F, T>): HConsK<F, T, A>

    fun size(): Int = foldRec<F, Int>(0 to this) { _, _ -> 1 }.first

    fun <R> fold(initV: R, f: (Kind<F, *>, R) -> R): R =
            foldRec(initV to this) { v, r -> f(v, r) }.first

    fun find(f: (Kind<F, *>) -> Boolean): Kind<F, *>? =
            fold<Kind<F, *>?>(null) { v, r -> if (r == null && f(v)) v else r }

    fun toList(): List<Kind<F, *>> = fold(LinkedList<Kind<F, *>>())
    { repo, list -> list.addFirst(repo); list }.toList()

    companion object {
        fun <F> nil(): HNilK<F> = HNilK()

        fun <F, E, L: HListK<F, L>> cons(e: Kind<F, E>, l: L): HConsK<F, E, L> = HConsK(e, l)

        fun <F, E> single(e: Kind<F, E>): HConsK<F, E, HNilK<F>> = HConsK(e, nil())
    }
}


class HNilK<F> : HListK<F, HNilK<F>>() {
    override fun <T> extend(e: Kind<F, T>): HConsK<F, T, HNilK<F>> = cons(e, this)
}

data class HConsK<F, E, L: HListK<F, L>>(val head: Kind<F, E>, val tail: L) : HListK<F, HConsK<F, E, L>>() {
    override fun <T> extend(e: Kind<F, T>): HConsK<F, T, HConsK<F, E, L>> = cons(e, this)
}

fun <F, T> Kind<F, T>.toHListK(): HConsK<F, T, HNilK<F>> = HListK.single(this)

fun <F, T> hlistKOf(v1: Kind<F, T>): HConsK<F, T, HNilK<F>> = HListK.single(v1)

fun <F, T1, T2> hlistKOf(v1: Kind<F, T1>, v2: Kind<F, T2>): HConsK<F, T2, HConsK<F, T1, HNilK<F>>> =
        HListK.single(v1).extend(v2)

fun <F, T1, T2, T3> hlistKOf(v1: Kind<F, T1>, v2: Kind<F, T2>, v3: Kind<F, T3>)
        : HConsK<F, T3, HConsK<F, T2, HConsK<F, T1, HNilK<F>>>> =
        HListK.single(v1).extend(v2).extend(v3)

fun <F, T1, T2, T3, T4> hlistKOf1(v1: Kind<F, T1>, v2: Kind<F, T2>, v3: Kind<F, T3>, v4: Kind<F, T4>)
        : HConsK<F, T4, HConsK<F, T3, HConsK<F, T2, HConsK<F, T1, HNilK<F>>>>> =
        HListK.single(v1).extend(v2).extend(v3).extend(v4)

fun <F, T1, T2, T3, T4, T5> hlistKOf(v1: Kind<F, T1>, v2: Kind<F, T2>, v3: Kind<F, T3>, v4: Kind<F, T4>, v5: Kind<F, T5>)
        : HConsK<F, T5, HConsK<F, T4, HConsK<F, T3, HConsK<F, T2, HConsK<F, T1, HNilK<F>>>>>> =
        HListK.single(v1).extend(v2).extend(v3).extend(v4).extend(v5)


private typealias FoldData<F, T> = Pair<T, HListK<F, *>>

private tailrec fun <F, T> foldRec(data: FoldData<F, T>, f: (Kind<F, *>, T) -> T): FoldData<F, T> {
    val (value, list) = data
    return when(list) {
        is HNilK -> data
        is HConsK<F, *, *> -> foldRec(f(list.head, value) to (list.tail as HListK<F, *>), f)
    }
}

/**
 * Mustache:
 *
 * data: {"index": 4, "sum": 5, "list":["1", "2", "3", "4", "5"]}
 *
 * template:
 * fun <F, {{#list}}T{{.}}, {{/list}}L : HListK<F, L>> {{#list}}HConsK<F, T{{.}}, {{/list}}L{{#list}}>{{/list}}
 *         .get{{index}}(): Kind<F, T{{sum}}> = {{#list}}{{^-last}}tail.{{/-last}}{{/list}}head
 */
fun <F, T, L : HListK<F, L>> HConsK<F, T, L>.get0(): Kind<F, T> = head

fun <F, T1, T2, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, L>>.get1(): Kind<F, T2> = tail.head

fun <F, T1, T2, T3, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, L>>>.get2(): Kind<F, T3> = tail.tail.head

fun <F, T1, T2, T3, T4, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, L>>>>.get3(): Kind<F, T4> = tail.tail.tail.head

fun <F, T1, T2, T3, T4, T5, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, L>>>>>
        .get4(): Kind<F, T5> = tail.tail.tail.tail.head

fun <F, T1, T2, T3, T4, T5, T6, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, L>>>>>>
        .get5(): Kind<F, T6> = tail.tail.tail.tail.tail.head

fun <F, T1, T2, T3, T4, T5, T6, T7, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, L>>>>>>>
        .get6(): Kind<F, T7> = tail.tail.tail.tail.tail.tail.head

fun <F, T1, T2, T3, T4, T5, T6, T7, T8, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, L>>>>>>>>
        .get7(): Kind<F, T8> = tail.tail.tail.tail.tail.tail.tail.head

fun <F, T1, T2, T3, T4, T5, T6, T7, T8, T9, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, L>>>>>>>>>
        .get8(): Kind<F, T9> = tail.tail.tail.tail.tail.tail.tail.tail.head

fun <F, T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, L : HListK<F, L>> HConsK<F, T0, HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, L>>>>>>>>>>
        .get9(): Kind<F, T9> = tail.tail.tail.tail.tail.tail.tail.tail.tail.head

fun <F, T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, L : HListK<F, L>> HConsK<F, T0, HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, HConsK<F, T10, L>>>>>>>>>>>
        .get10(): Kind<F, T10> = tail.tail.tail.tail.tail.tail.tail.tail.tail.tail.head

/**
 * Mustache:
 *
 * data: {"index": 4, "sum": 5, "list":["1", "2", "3", "4", "5"]}
 *
 * template:
 * fun <F, R, {{#list}}T{{.}}, {{/list}}L : HListK<F, L>> {{#list}}HConsK<F, T{{.}}, {{/list}}L{{#list}}>{{/list}}.map{{index}}(f: (Kind<F, T{{sum}}>) -> Kind<F, R>)
 *         : {{#list}}{{^-last}}HConsK<F, T{{.}}, {{/-last}}{{/list}}HConsK<F, R, L{{#list}}>{{/list}} =
 *         {{#list}}{{^-last}}tail.{{/-last}}{{/list}}tail
 *                 .extend(f({{#list}}{{^-last}}tail.{{/-last}}{{/list}}head))
 * {{#list}}
 *                 .extend({{#list}}{{^-last}}tail.{{/-last}}{{/list}}head)
 * {{/list}}
 */
fun <F, R, T, L : HListK<F, L>> HConsK<F, T, L>.map0(f: (Kind<F, T>) -> Kind<F, R>): HConsK<F, R, L> = tail.extend(f(head))

fun <F, R, T1, T2, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, L>>.map1(f: (Kind<F, T2>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, R, L>> =
        tail.tail.extend(f(tail.head)).extend(head)

fun <F, R, T1, T2, T3, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, L>>>.map2(f: (Kind<F, T3>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, R, L>>> =
        tail.tail.tail
                .extend(f(tail.tail.head))
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, L>>>>.map3(f: (Kind<F, T4>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, R, L>>>> =
        tail.tail.tail.tail
                .extend(f(tail.tail.tail.head))
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, L : HListK<F, L>>
        HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, L>>>>>.map4(f: (Kind<F, T5>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, R, L>>>>> =
        tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.head))
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, T6, L : HListK<F, L>>
        HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, L>>>>>>.map5(f: (Kind<F, T6>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, R, L>>>>>> =
        tail.tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.tail.head))
                .extend(tail.tail.tail.tail.head)
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, T6, T7, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, L>>>>>>>.map6(f: (Kind<F, T7>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, R, L>>>>>>> =
        tail.tail.tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.tail.tail.head))
                .extend(tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.head)
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, T6, T7, T8, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, L>>>>>>>>.map7(f: (Kind<F, T8>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, R, L>>>>>>>> =
        tail.tail.tail.tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.tail.tail.tail.head))
                .extend(tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.head)
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, T6, T7, T8, T9, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, L>>>>>>>>>.map8(f: (Kind<F, T9>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, R, L>>>>>>>>> =
        tail.tail.tail.tail.tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.tail.tail.tail.tail.head))
                .extend(tail.tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.head)
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, HConsK<F, T10, L>>>>>>>>>>.map9(f: (Kind<F, T10>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, HConsK<F, R, L>>>>>>>>>> =
        tail.tail.tail.tail.tail.tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.tail.tail.tail.tail.tail.head))
                .extend(tail.tail.tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.head)
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)

fun <F, R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, L : HListK<F, L>> HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, HConsK<F, T10, HConsK<F, T11, L>>>>>>>>>>>.map10(f: (Kind<F, T11>) -> Kind<F, R>)
        : HConsK<F, T1, HConsK<F, T2, HConsK<F, T3, HConsK<F, T4, HConsK<F, T5, HConsK<F, T6, HConsK<F, T7, HConsK<F, T8, HConsK<F, T9, HConsK<F, T10, HConsK<F, R, L>>>>>>>>>>> =
        tail.tail.tail.tail.tail.tail.tail.tail.tail.tail.tail
                .extend(f(tail.tail.tail.tail.tail.tail.tail.tail.tail.tail.head))
                .extend(tail.tail.tail.tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.tail.head)
                .extend(tail.tail.tail.tail.head)
                .extend(tail.tail.tail.head)
                .extend(tail.tail.head)
                .extend(tail.head)
                .extend(head)