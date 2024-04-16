package top.fumiama.copymanga.tools.http

import android.content.Context
import android.util.Base64
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.Gson
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.ComandyCapsule
import java.nio.ByteBuffer

@GlideModule
class ComandyGlideModule: AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        registry.prepend(GlideUrl::class.java, ByteBuffer::class.java, ComandyGlideUrlFactory())
        registry.prepend(String::class.java, ByteBuffer::class.java, ComandyStringFactory())
    }

    inner class ComandyDataFetcher(private val model: GlideUrl): DataFetcher<ByteBuffer> {
        constructor(model: String): this(GlideUrl(model))
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in ByteBuffer>
        ) {
            val capsule = ComandyCapsule()
            capsule.url = model.toStringUrl()
            capsule.method = "GET"
            if (model.headers.isNotEmpty()) {
                capsule.headers = hashMapOf()
                model.headers.forEach { (k, v) -> capsule.headers[k] = v }
            }
            try {
                val para = Gson().toJson(capsule)
                Comandy.instance!!.request(para).let { result ->
                    Gson().fromJson(result, ComandyCapsule::class.java)!!.let {
                        if (it.code != 200) {
                            callback.onLoadFailed(IllegalArgumentException("HTTP${it.code} ${
                                it.data?.let { d -> Base64.decode(d, Base64.DEFAULT).decodeToString() }
                            }"))
                            return
                        }
                        callback.onDataReady(ByteBuffer.wrap(Base64.decode(it.data, Base64.DEFAULT)))
                    }
                }
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }

        override fun cleanup() { }

        override fun cancel() { }

        override fun getDataClass(): Class<ByteBuffer> {
            return ByteBuffer::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.REMOTE
        }

    }

    inner class ComandyGlideUrlModelLoader: ModelLoader<GlideUrl, ByteBuffer> {
        override fun buildLoadData(
            model: GlideUrl,
            width: Int,
            height: Int,
            options: Options
        ): ModelLoader.LoadData<ByteBuffer> {
            return ModelLoader.LoadData(ObjectKey(model), ComandyDataFetcher(model))
        }

        override fun handles(model: GlideUrl): Boolean {
            return Comandy.useComandy && Comandy.instance != null && model.toURL().let {
                it.protocol == "https" && it.host != "copymanga.azurewebsites.net"
            }
        }

    }

    inner class ComandyStringModelLoader: ModelLoader<String, ByteBuffer> {
        override fun buildLoadData(
            model: String,
            width: Int,
            height: Int,
            options: Options
        ): ModelLoader.LoadData<ByteBuffer> {
            return ModelLoader.LoadData(ObjectKey(model), ComandyDataFetcher(model))
        }

        override fun handles(model: String): Boolean {
            return Comandy.useComandy && Comandy.instance != null && model.startsWith("https://")
        }

    }

    inner class ComandyGlideUrlFactory: ModelLoaderFactory<GlideUrl, ByteBuffer> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, ByteBuffer> {
            return ComandyGlideUrlModelLoader()
        }

        override fun teardown() { }

    }

    inner class ComandyStringFactory: ModelLoaderFactory<String, ByteBuffer> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, ByteBuffer> {
            return ComandyStringModelLoader()
        }

        override fun teardown() { }

    }
}