package org.lsposed.corepatch.hook

import org.lsposed.corepatch.Config

object NtConfigListServiceImplHook : BaseHook() {
    override val name = "NtConfigListServiceImplHook"

    override fun hook() {
        // TODO: Check is Nothing Phone
        val ntConfigListServiceImplClazz = try {
            "com.nothing.server.ex.NtConfigListServiceImpl".toClass()
        } catch (e: ClassNotFoundException) {
            return
        }
        val isInstallingAppForbiddenMethod =
            ntConfigListServiceImplClazz.declaredMethods.first { m -> m.name == "isInstallingAppForbidden" }
        isInstallingAppForbiddenMethod.hook {
            before {
                if (Config.isBypassBlockEnabled()) result = false
            }
        }
        val isStartingAppForbiddenMethod =
            ntConfigListServiceImplClazz.declaredMethods.first { m -> m.name == "isStartingAppForbidden" }
        isStartingAppForbiddenMethod.hook {
            before {
                if (Config.isBypassBlockEnabled()) result = false
            }
        }
    }
}
