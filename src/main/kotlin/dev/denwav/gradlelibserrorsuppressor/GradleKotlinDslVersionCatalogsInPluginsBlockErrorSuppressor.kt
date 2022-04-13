package dev.denwav.gradlelibserrorsuppressor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.patterns.StandardPatterns.string
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.plugins.gradle.util.GradleUtil

class GradleKotlinDslVersionCatalogsInPluginsBlockErrorSuppressor : HighlightInfoFilter {

    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        if (file == null) {
            return true
        }

        if (file.fileType != KotlinFileType.INSTANCE) {
            return true
        }

        val errorDesc = highlightInfo.description ?: return true
        if (!errorMessagePattern.matches(errorDesc)) {
            return true
        }

        val element = file.findElementAt(highlightInfo.startOffset) ?: return true

        val gradleVersion = GradleUtil.getGradleVersion(element.project, file)
        if (gradleVersion < minimumGradleVersion) {
            return true
        }

        val property = getProperty(element) ?: return true

        if (!property.isTopLevel) {
            return true
        }

        if (!receiverTypeIsProject(property)) {
            return true
        }

        return !typeImplementsExternalModuleDependencyFactory(property)
    }

    private fun getProperty(element: PsiElement): KtProperty? {
        if (element !is LeafPsiElement) {
            return null
        }

        val ktFile = element.containingFile as? KtFile ?: return null
        if (!ktFile.isScript()) {
            return null
        }
        if (!ktFile.name.endsWith(".gradle.kts")) {
            return null
        }

        if (element.elementType != KtTokens.IDENTIFIER) {
            return null
        }

        if (!ktScriptInitPattern.accepts(element)) {
            return null
        }

        val parent = element.parent as? KtNameReferenceExpression ?: return null
        val reference = parent.references.firstOrNull { it is KtSimpleNameReference } ?: return null

        return reference.resolve() as? KtProperty
    }

    private fun receiverTypeIsProject(property: KtProperty): Boolean {
        val receiverType = property.receiverTypeReference ?: return false
        val typeElement = receiverType.typeElement as? KtUserType ?: return false
        return typeElement.text == GRADLE_PROJECT
    }

    private fun typeImplementsExternalModuleDependencyFactory(property: KtProperty): Boolean {
        val type = property.typeReference ?: return false
        val typeElement = type.typeElement as? KtUserType ?: return false
        val typeName = typeElement.text ?: return false

        val project = property.project
        val facade = JavaPsiFacade.getInstance(project)
        val psiClass = facade.findClass(typeName, GlobalSearchScope.everythingScope(project)) ?: return false

        return InheritanceUtil.isInheritor(psiClass, CATALOG_TYPE)
    }

    companion object {
        private const val GRADLE_PROJECT = "org.gradle.api.Project"
        private const val CATALOG_TYPE = "org.gradle.api.internal.catalog.ExternalModuleDependencyFactory"

        private val minimumGradleVersion = GradleVersion.version("7.2")

        private val errorMessagePattern = Regex("\\[DSL_SCOPE_VIOLATION] 'val Project\\.\\w+: \\w+' " +
            "can't be called in this context by implicit receiver\\. Use the explicit one if necessary")

        private val ktScriptInitPattern =
            psiElement<LeafPsiElement>()
                .inside(
                    true,
                    psiElement<KtNameReferenceExpression>()
                        .inside(
                            true,
                            // plugins { ... }
                            psiElement<KtScriptInitializer>()
                                .withFirstChild(
                                    psiElement<KtCallExpression>()
                                        .withFirstChild(
                                            psiElement<KtNameReferenceExpression>()
                                                .withText("plugins")
                                        )
                                )
                        )
                )
                .inFile(psiFile<KtFile>().withName(string().endsWith(".gradle.kts")))
    }
}
