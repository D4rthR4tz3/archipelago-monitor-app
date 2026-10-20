package io.github.d4rthr4tz3.apviewer.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import io.github.d4rthr4tz3.apviewer.protocol.store.AppSettings
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.protocol.store.StoreCodec
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException

/** Saved multiworlds, slots and settings, stored as JSON in a DataStore file. */
class ProfilesRepository(private val dataStore: DataStore<Profiles>) {
    val profiles: Flow<Profiles> = dataStore.data

    /** Adds [multiworld], or replaces the saved one with the same id. */
    suspend fun saveMultiworld(multiworld: SavedMultiworld) {
        dataStore.updateData { it.withMultiworld(multiworld) }
    }

    suspend fun removeMultiworld(id: String) {
        dataStore.updateData { it.withoutMultiworld(id) }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.updateData { it.copy(settings = transform(it.settings)) }
    }

    companion object {
        private const val FILE_NAME = "profiles.json"

        fun create(context: Context): ProfilesRepository = ProfilesRepository(
            DataStoreFactory.create(
                serializer = ProfilesSerializer,
                // An unreadable file would otherwise block the app on every start.
                corruptionHandler = ReplaceFileCorruptionHandler { Profiles() },
                produceFile = { context.dataStoreFile(FILE_NAME) },
            ),
        )
    }
}

private object ProfilesSerializer : Serializer<Profiles> {
    override val defaultValue: Profiles = Profiles()

    override suspend fun readFrom(input: InputStream): Profiles = try {
        StoreCodec.decodeProfiles(input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read profiles", e)
    }

    override suspend fun writeTo(t: Profiles, output: OutputStream) {
        output.write(StoreCodec.encodeProfiles(t).encodeToByteArray())
    }
}
