package dev.nick.stepcounter.domain.calculator

private const val STRIDE = 0.00414
private const val KM = 0.001


fun calculateMeters(steps: Int, height: Int): Double {
        return steps * height * STRIDE
}
fun calculateKm(steps: Int, height: Int):Double {
return steps * height * STRIDE * KM

}

