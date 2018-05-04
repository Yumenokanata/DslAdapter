# DslAdapter
A RecyclerView Adapter builder by DSL. Easy to use, and all code written by kotlin.

---

Now you can simply build a complex RecyclerView structure, just by:

```kotlin
val adapter = RendererAdapter.repositoryAdapter()
        .addStaticItem(layout<Unit>(R.layout.list_header))
        .add({ none<List<ItemModel>>() },
                optionRenderer(
                        noneItemRenderer = LayoutRenderer.dataBindingItem<Unit, ItemLayoutBinding>(
                                count = 5,
                                layout = R.layout.item_layout,
                                bindBinding = { ItemLayoutBinding.bind(it) },
                                binder = { bind, item, _ ->
                                    bind.content = "this is empty item"
                                },
                                recycleFun = { it.model = null; it.content = null; it.click = null }),
                        itemRenderer = databindingOf<ItemModel>(R.layout.item_layout)
                                .itemId(BR.model)
                                .stableIdForItem({ it.id })
                                .handler(BR.click, { v: ItemModel ->
                                    Toast.makeText(this, "Click: ${v.content}", LENGTH_SHORT).show()
                                })
                                .forList()
                ))
        .add({ provideData(index) },
                LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
                        stableIdForItem = { item, index -> item.id },
                        binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = itemModel.title },
                        recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                        .forList({ i, index -> index }))
        .add({ provideData(index) },
                databindingOf<ItemModel>(R.layout.item_layout)
                        .onRecycle(CLEAR_ALL)
                        .itemId(BR.model)
                        .itemId(BR.content, { m -> m.content + "xxxx" })
                        .stableIdForItem { it.id }
                        .checkKey { i, index -> index }
                        .forList())
        .addItem(DateFormat.getInstance().format(Date()),
                databindingOf<String>(R.layout.list_footer)
                        .itemId(BR.text)
                        .forItem())
        .build()
```

---

### Base Renderer

1. **LayoutRenderer**
2. **ConstantItemRenderer**: like `LayoutRenderer`, but not check data update.
3. **EmptyRenderer**
4. **GroupItemRenderer** : A title item with list items, just like:   

| Group title |
| ----------- |
| sub item 1  |
| sub item 2  |
| ...         |

5. **ListRenderer** : Make list for other renderer:
6. **SealedItemRenderer** : Choice different Renderer for different data.
7. **DataBindingRenderer** : Bind with Android Databinding

Use this Base Renderers, you can Make a complex RecyclerView structure:

```
|--LayoutRenderer  header
|
|--SealedItemRenderer
|    |--none -> LayoutRenderer placeholder count 5
|    | 
|    |--some -> ListRenderer
|                 |--DataBindingRenderer 1
|                 |--DataBindingRenderer 2
|                 |--... 
|
|--ListRenderer
|    |--LayoutRenderer simple item1
|    |--LayoutRenderer simple item2
|    |--...
|
|--ListRenderer
|    |--DataBindingRenderer item with content1
|    |--DataBindingRenderer item with content2
|    |--...
|
|--DataBindingRenderer footer
```

### Custom Renderer

Base Renderer is `Renderer<Data, VD: ViewData>`
```kotlin
interface Renderer<Data, VD: ViewData> {
    fun getData(content: Data): VD

    fun getItemId(data: VD, index: Int): Long = RecyclerView.NO_ID

    fun getItemViewType(data: VD, position: Int): Int

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    fun bind(data: VD, index: Int, holder: RecyclerView.ViewHolder)

    fun recycle(holder: RecyclerView.ViewHolder)

    fun getUpdates(oldData: VD, newData: VD): List<UpdateActions>
}

interface ViewData {
    val count: Int
}
```

Implement `Renderer`interface, you can make your own renderer.


Or make Your Renderer Simpler with `BaseRenderer<T, VD: ViewData>`
```kotlin
abstract class BaseRenderer<T, VD: ViewData> : Renderer<T, VD> {

    override fun getItemViewType(data: VD, position: Int): Int = getLayoutResId(data, position)

    override fun getItemId(data: VD, index: Int): Long {
        return -1L
    }

    @LayoutRes
    abstract fun getLayoutResId(data: VD, position: Int): Int


    override fun recycle(holder: RecyclerView.ViewHolder) {}

    override fun onCreateViewHolder(parent: ViewGroup, layoutResourceId: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layoutResourceId, parent, false)) {

        }
    }
}
```

### Data Updates

A simple Adapter:
```kotlin
val adapter = RendererAdapter.repositoryAdapter()
        .add({ provideData(index) },
                databindingOf<ItemModel>(R.layout.item_layout)
                        .onRecycle(CLEAR_ALL)
                        .itemId(BR.model)
                        .itemId(BR.content, { m -> m.content + "xxxx" })
                        .stableIdForItem { it.id }
                        .checkKey { i, index -> index }
                        .forList())
        .addItem(DateFormat.getInstance().format(Date()),
                databindingOf<String>(R.layout.list_footer)
                        .itemId(BR.text)
                        .forItem())
        .build()
```

If your data has chenged, you want to update Adapter, You have two ways to update the Adapter:
1. RendererAdapter.forceUpdateAdapter(): Unit
  this method will calculate new ViewData, and call `notifyDataSetChanged()` force refresh all items
```kotlin
adapter.forceUpdateAdapter()
```

2. **RendererAdapter.autoUpdateAdapter(): List<UpdateActions>**
  this method will update ViewData, and calculate the difference between old and new data, return Update Actions, and you can dispatch updates to Adapter.
```kotlin
adapter.autoUpdateAdapter()
    .dispatchUpdatesTo(adapter)
```
**Tip**: method `autoUpdateAdapter()`calculation takes some time, so you can call `autoUpdateAdapter()` at computation thread, and call `dispatchUpdatesTo()` at android main thread. just like:

```kotlin
Single.fromCallable { adapter.autoUpdateAdapter() }
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Consumer { it.dispatchUpdatesTo(adapter) })
```

### Auto Updates

You can see `getUpdates()` method:
```kotlin
fun getUpdates(oldData: VD, newData: VD): List<UpdateActions>
```

By this method, Adapter can auto calculate  the difference between two lists and output a list of update operations that converts the first list into the second one.

You can also customize your own calculation method.

  

### License
<pre>
Copyright 2018 Yumenokanata

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
