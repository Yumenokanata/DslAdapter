package indi.yume.tools.dsladapter

import androidx.annotation.MainThread
import indi.yume.tools.dsladapter.datatype.HConsK
import indi.yume.tools.dsladapter.datatype.HListK
import indi.yume.tools.dsladapter.datatype.HNilK
import indi.yume.tools.dsladapter.renderers.*
import indi.yume.tools.dsladapter.typeclass.BaseRenderer
import indi.yume.tools.dsladapter.typeclass.ViewData


typealias Supplier<T> = () -> T

fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>
        RendererAdapter.Companion.singleSupplier(supplier: Supplier<T>, renderer: BR):
        Pair<RendererAdapter<T, VD, BR>, SupplierController<T, VD, BR>> =
        RendererAdapter.singleRenderer(supplier(), renderer).let { it to SupplierController(it, supplier) }

fun RendererAdapter.Companion.supplierBuilder()
        : SupplierBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> =
        SupplierBuilder.start

class SupplierController<T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>(
        val adapter: RendererAdapter<T, VD, BR>,
        val supplierSum: Supplier<T>
) {
    @MainThread
    fun updateAll() {
        adapter.setData(supplierSum())
    }
}

class SupplierBuilder<DL : HListK<ForIdT, DL>, IL : HListK<ForComposeItem, IL>, VDL : HListK<ForComposeItemData, VDL>>(
        val supplierSum: Supplier<DL>,
        val initSumData: DL,
        val composeBuilder: ComposeBuilder<DL, IL, VDL>
) {
    fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>
            addStatic(initData: T, renderer: BR)
            : SupplierBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, BR>, IL>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>> =
            SupplierBuilder({ supplierSum().extend(initData.toIdT()) }, initSumData.extend(IdT(initData)), composeBuilder.add(renderer))

    fun <T, VD : ViewData<T>, BR : BaseRenderer<T, VD>>
            add(supplier: Supplier<T>, renderer: BR)
            : SupplierBuilder<HConsK<ForIdT, T, DL>, HConsK<ForComposeItem, Pair<T, BR>, IL>, HConsK<ForComposeItemData, Pair<T, BR>, VDL>> =
            SupplierBuilder({ supplierSum().extend(supplier().toIdT()) }, initSumData.extend(IdT(supplier())), composeBuilder.add(renderer))

    fun <VD : ViewData<Unit>, BR : BaseRenderer<Unit, VD>>
            addStatic(renderer: BR)
            : SupplierBuilder<HConsK<ForIdT, Unit, DL>, HConsK<ForComposeItem, Pair<Unit, BR>, IL>, HConsK<ForComposeItemData, Pair<Unit, BR>, VDL>> =
            SupplierBuilder({ supplierSum().extend(Unit.toIdT()) }, initSumData.extend(IdT(Unit)), composeBuilder.add(renderer))

    fun build(): Pair<RendererAdapter<DL, ComposeViewData<DL, VDL>, ComposeRenderer<DL, IL, VDL>>,
            SupplierController<DL, ComposeViewData<DL, VDL>, ComposeRenderer<DL, IL, VDL>>> =
            RendererAdapter(initSumData, composeBuilder.build()).let { it to SupplierController(it, supplierSum) }

    companion object {
        val start: SupplierBuilder<HNilK<ForIdT>, HNilK<ForComposeItem>, HNilK<ForComposeItemData>> =
                SupplierBuilder({ HListK.nil() }, HListK.nil(), ComposeRenderer.startBuild)
    }
}

