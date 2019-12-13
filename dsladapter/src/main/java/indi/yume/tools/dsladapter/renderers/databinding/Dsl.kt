package indi.yume.tools.dsladapter.renderers.databinding

import android.view.View
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import indi.yume.tools.dsladapter.renderers.LayoutRenderer

fun <T : Any, DB : ViewDataBinding> LayoutRenderer.Companion.dataBindingItem(
        count: Int = 1,
        @LayoutRes layout: Int,
        bindBinding: (View) -> DB,
        binder: (DB, T, Int) -> Unit = { _, _, _ -> },
        recycleFun: (DB) -> Unit = { }) =
        LayoutRenderer<T>(
                count = count,
                layout = layout,
                binder = { view, data, index ->
                    val binding = DataBindingUtil.getBinding(view)
                            ?: bindBinding(view)
                    binder(binding, data, index)
                    binding.executePendingBindings()
                },
                recycleFun =
                { view ->
                    val binding = DataBindingUtil.getBinding(view)
                            ?: bindBinding(view)
                    recycleFun(binding)
                    binding.executePendingBindings()
                })
