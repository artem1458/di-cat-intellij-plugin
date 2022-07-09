package com.github.artem1458.dicatplugin

import com.intellij.util.messages.Topic

interface DICatActionNotifier {
  val DI_CAT_TOPIC: Topic<DICatActionNotifier>
    get() = Topic.create("DI Cat actions topic", DICatActionNotifier::class.java)
}

