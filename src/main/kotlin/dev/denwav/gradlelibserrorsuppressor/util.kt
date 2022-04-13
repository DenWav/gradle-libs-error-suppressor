package dev.denwav.gradlelibserrorsuppressor

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.PsiFilePattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

inline fun <reified T : PsiElement> psiElement(): PsiElementPattern.Capture<T> = psiElement(T::class.java)
inline fun <reified T : PsiFile> psiFile(): PsiFilePattern.Capture<T> = psiFile(T::class.java)
