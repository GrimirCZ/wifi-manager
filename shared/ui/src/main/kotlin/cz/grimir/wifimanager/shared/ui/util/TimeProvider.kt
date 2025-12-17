package cz.grimir.wifimanager.shared.ui.util

import java.time.Instant
import java.util.function.Supplier

fun interface TimeProvider : Supplier<Instant>
