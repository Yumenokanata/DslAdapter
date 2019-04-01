package indi.yume.tools.dsladapter.renderers

import androidx.recyclerview.widget.RecyclerView
import indi.yume.tools.dsladapter.ActionU
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData
import indi.yume.tools.dsladapter.updateVD

class MockRenderer<T>(val counter: (T) -> Int) : BaseRenderer<T, MockViewData<T>>() {
    override fun getData(content: T): MockViewData<T> = MockViewData(content, counter(content))

    override fun getItemViewType(data: MockViewData<T>, position: Int): Int =
            throw UnsupportedOperationException("MockRenderer do not support getItemViewType.")

    override fun getLayoutResId(data: MockViewData<T>, position: Int): Int =
            throw UnsupportedOperationException("MockRenderer do not support getLayoutResId.")

    override fun bind(data: MockViewData<T>, index: Int, holder: RecyclerView.ViewHolder) =
            throw UnsupportedOperationException("MockRenderer do not support bind.")

    override fun recycle(holder: RecyclerView.ViewHolder) =
            throw UnsupportedOperationException("MockRenderer do not support recycle.")
}

data class MockViewData<T>(override val originData: T, override val count: Int) : ViewData<T>

class MockUpdater<T>(val renderer: MockRenderer<T>) {
    fun update(t: T): ActionU<MockViewData<T>> = { oldVD ->
        val newVD = MockViewData(t, renderer.counter(t))
        updateVD(oldVD, newVD) to newVD
    }
}