package indi.yume.tools.dsladapter

import androidx.annotation.MainThread
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


typealias Supplier<T> = () -> T

fun <T, VD : ViewData<T>>
        RendererAdapter.Companion.singleSupplier(supplier: Supplier<T>, renderer: BaseRenderer<T, VD>):
        Pair<RendererAdapter<T, VD>, SupplierController<T, VD>> =
        RendererAdapter.singleRenderer(supplier(), renderer).let { it to SupplierController(it, supplier) }

fun RendererAdapter.Companion.supplierBuilder()
        : SupplierBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> =
        SupplierBuilder.start

class SupplierController<T, VD : ViewData<T>>(
        val adapter: RendererAdapter<T, VD>,
        val supplierSum: Supplier<T>
) {
    @MainThread
    fun updateAll() {
        adapter.setData(supplierSum())
    }
}

class SupplierBuilder<DL : HListK<ForIdT, DL>, VDL : HListK<ForComposeItemData, VDL>>(
        val supplierSum: Supplier<DL>,
        val initSumData: DL,
        val composeBuilder: ComposeBuilder<DL, VDL>
) {
    fun <T, VD : ViewData<T>>
            addStatic(initData: T, renderer: BaseRenderer<T, VD>)
            : SupplierBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            SupplierBuilder({ supplierSum().extend(initData.toIdT()) }, initSumData.extend(IdT(initData)), composeBuilder.add(renderer))

    fun <T, VD : ViewData<T>>
            add(supplier: Supplier<T>, renderer: BaseRenderer<T, VD>)
            : SupplierBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItemData, Pair<T, VD>, VDL>> =
            SupplierBuilder({ supplierSum().extend(supplier().toIdT()) }, initSumData.extend(IdT(supplier())), composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>>
            addStatic(renderer: BaseRenderer<Unit, VD>)
            : SupplierBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItemData, Pair<Unit, VD>, VDL>> =
            SupplierBuilder({ supplierSum().extend(Unit.toIdT()) }, initSumData.extend(IdT(Unit)), composeBuilder.add(renderer))

    fun build(): Pair<RendererAdapter<DL, ComposeViewData<DL, VDL>>,
            SupplierController<DL, ComposeViewData<DL, VDL>>> =
            RendererAdapter(initSumData, composeBuilder.build()).let { it to SupplierController(it, supplierSum) }

    companion object {
        val start: SupplierBuilder<HNilK<ForIdT>, HNilK<ForComposeItemData>> =
                SupplierBuilder({ HListK.nil() }, HListK.nil(), ComposeRenderer.startBuild)
    }
}

