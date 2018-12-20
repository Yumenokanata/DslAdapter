package indi.yume.tools.sample

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//        println(fibonacci(0))
        Observable.fromCallable {
            println("1")
            listOf("1", "2")
        }
                .doOnSubscribe { disposable -> println("2") }
                .doOnNext { l -> println("3") }
                .flatMap { l ->
                    Observable.fromIterable(l)
                            .doOnSubscribe { disposable -> println("4") }
                            .doOnNext { s -> println("5") }
                }
                .doOnSubscribe { disposable -> println("6") }
                .subscribeOn(Schedulers.io())
                .doOnNext { s -> println("7") }
                .map { s -> s + "map" }
                .doOnSubscribe { disposable -> println("8") }
                .doOnNext { s -> println("9") }
                .blockingSubscribe()

    }

    fun fibonacci(index: Int): Int {
        tailrec fun fibonacci_help(last: Int, next: Int, i: Int): Int =
                when (i) {
                    0 -> last
                    else -> fibonacci_help(next, last + next, i - 1)
                }

        return fibonacci_help(0, 1, index)
    }


}

interface ViewPagerController {
    // ViewPager中Item数量改变的事件, 大于等于0
    val viewPagerCountObs: Observable<Int>

    // ViewPager控件的触控事件
    val viewPagerTouchEventObs: Observable<MotionAction>

    // 当前显示的Item的Index, 如果没有内容则返回-1
    fun getCurrentItemIndex(): Int

    // 设定需要显示的Item的Index, 0开始
    fun setCurrentItem(index: Int): Unit
}

sealed class MotionAction
object ActionDown : MotionAction()
object ActionMove : MotionAction()
object ActionUp : MotionAction()


fun buildBannerController(controller: ViewPagerController): Completable =
    controller.viewPagerCountObs
            .switchMap { count ->
                Observable.merge(Observable.just(true),
                        controller.viewPagerTouchEventObs
                                .map { it is ActionUp })
                        .flatMap {
                            if (it)
                                Observable.timer(4, TimeUnit.SECONDS)
                            else
                                Observable.empty()
                        }
                        .map {
                            val currentIndex = controller.getCurrentItemIndex()
                            when {
                                count <= 0 -> -1
                                controller.getCurrentItemIndex() >= count -> 0
                                else -> currentIndex + 1
                            }
                        }
                        .doOnNext { next ->
                            if (next != -1)
                                controller.setCurrentItem(next)
                        }
            }
            .ignoreElements()
