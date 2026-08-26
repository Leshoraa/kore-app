package com.leshoraa.kore.domain.model

/**
 * Represents a single secondary candidate target tracked by the embedded vision pipeline.
 *
 * @property index Candidate slot index (0, 1, 2).
 * @property cx Bounding box center X coordinate in camera sensor frame space.
 * @property cy Bounding box center Y coordinate in camera sensor frame space.
 * @property w Bounding box width in pixels.
 * @property h Bounding box height in pixels.
 * @property priority Motion and skin clustering priority score.
 */
data class TargetCandidate(
    val index: Int,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val priority: Float
)

/**
 * Complete real-time telemetry snapshot emitted by the KoRe firmware vision pipeline.
 *
 * @property detected Indicates if a valid primary target is currently locked.
 * @property cx Primary target center X coordinate in sensor coordinates.
 * @property cy Primary target center Y coordinate in sensor coordinates.
 * @property w Primary target bounding box width.
 * @property h Primary target bounding box height.
 * @property errX Horizontal angular/pixel gaze error relative to optical center.
 * @property errY Vertical angular/pixel gaze error relative to optical center.
 * @property conf Kalman tracking confidence metric (0.0 to 1.0).
 * @property fpsAi Embedded vision pipeline frame rate on Core 0.
 * @property humanLikelihood Chrominance skin probability score (0.0 to 1.0).
 * @property prox Target area proximity factor (0.0 to 1.0).
 * @property fw Native camera frame width in pixels (typically 640).
 * @property fh Native camera frame height in pixels (typically 480).
 * @property vx Estimated horizontal velocity in pixels/second.
 * @property vy Estimated vertical velocity in pixels/second.
 * @property numCands Total active spatial clusters identified in current frame.
 * @property inspIdx Candidate index currently selected for active gaze inspection.
 * @property candidates List of detected candidate targets.
 * @property expression Active facial expression index (0 to 7).
 * @property expressionName Name of the active facial expression.
 * @property isManual True if current expression is manually locked rather than auto-mood.
 * @property valence 2D Russell Circumplex emotional valence coordinate (-1.0 to 1.0).
 * @property arousal 2D Russell Circumplex emotional arousal coordinate (0.0 to 1.0).
 * @property heapFree Free internal SRAM heap in bytes.
 * @property psramFree Free external PSRAM in bytes.
 * @property uptimeSeconds Device uptime in seconds.
 * @property cpuMhz Current CPU operating frequency in MHz.
 */
data class TelemetryData(
    val detected: Boolean = false,
    val cx: Float = 0f,
    val cy: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
    val errX: Float = 0f,
    val errY: Float = 0f,
    val conf: Float = 0f,
    val fpsAi: Float = 0f,
    val humanLikelihood: Float = 0f,
    val prox: Float = 0f,
    val fw: Int = 640,
    val fh: Int = 480,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val numCands: Int = 0,
    val inspIdx: Int = 0,
    val candidates: List<TargetCandidate> = emptyList(),
    val expression: Int = 0,
    val expressionName: String = "IDLE",
    val isManual: Boolean = false,
    val valence: Float = 0f,
    val arousal: Float = 0f,
    val heapFree: Long = 0L,
    val psramFree: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val cpuMhz: Int = 240
)

/**
 * Adjustable camera sensor parameters supported by the KoRe firmware SCCB driver.
 */
data class CameraSensorParams(
    val brightness: Int = 0,     // Range: -2 to +2
    val contrast: Int = 0,       // Range: -2 to +2
    val saturation: Int = 0,     // Range: -2 to +2
    val vflip: Boolean = true,
    val hmirror: Boolean = true,
    val aec: Boolean = true,
    val agc: Boolean = true
)

/**
 * Operational state of the live MJPEG stream connection.
 */
enum class StreamConnectionState {
    IDLE,
    CONNECTING,
    STREAMING,
    ERROR
}
