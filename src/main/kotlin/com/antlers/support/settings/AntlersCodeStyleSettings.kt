package com.antlers.support.settings

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

class AntlersCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings("AntlersCodeStyleSettings", container) {

    @JvmField var SPACES_INSIDE_DELIMITERS = true
    @JvmField var SPACES_AROUND_PIPE = true
    @JvmField var SPACES_AROUND_OPERATORS = true
}
