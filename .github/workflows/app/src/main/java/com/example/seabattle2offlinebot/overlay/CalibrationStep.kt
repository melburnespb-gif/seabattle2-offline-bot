package com.example.seabattle2offlinebot.overlay

data class CalibrationStep(val key: String, val label: String) {
    companion object {
        fun defaultSteps() = listOf(
            // Универсальные кнопки (ты выбираешь сам)
            CalibrationStep("btn_action_1", "Кнопка действия №1 (например: открыть режим/играть)"),
            CalibrationStep("btn_action_2", "Кнопка действия №2 (например: начать/подтвердить)"),
            CalibrationStep("btn_action_3", "Кнопка действия №3 (например: случайная расстановка)"),
            CalibrationStep("btn_action_4", "Кнопка действия №4 (например: старт боя/поиск)"),
            CalibrationStep("btn_action_5", "Кнопка действия №5 (например: закрыть результат/далее)"),

            // Два поля (левое/правое) — чтобы определить где “цель”, где “своё”
            CalibrationStep("field_left_tl",  "ЛЕВОЕ поле: верхний-левый угол (внутри рамки)"),
            CalibrationStep("field_left_br",  "ЛЕВОЕ поле: нижний-правый угол (внутри рамки)"),
            CalibrationStep("field_right_tl", "ПРАВОЕ поле: верхний-левый угол (внутри рамки)"),
            CalibrationStep("field_right_br", "ПРАВОЕ поле: нижний-правый угол (внутри рамки)")
        )
    }
}
