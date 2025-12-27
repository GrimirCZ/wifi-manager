package cz.grimir.wifimanager.shared.core

import java.time.Instant
import java.util.function.Supplier

fun interface TimeProvider : Supplier<Instant>
