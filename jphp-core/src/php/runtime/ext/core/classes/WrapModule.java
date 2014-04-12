package php.runtime.ext.core.classes;

import org.develnext.jphp.core.compiler.jvm.JvmCompiler;
import php.runtime.Memory;
import php.runtime.common.HintType;
import php.runtime.env.Context;
import php.runtime.env.Environment;
import php.runtime.ext.core.stream.Stream;
import php.runtime.lang.BaseObject;
import php.runtime.loader.dump.ModuleDumper;
import php.runtime.memory.ArrayMemory;
import php.runtime.reflection.ClassEntity;
import php.runtime.reflection.ModuleEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static php.runtime.annotation.Reflection.*;

@Name("php\\lang\\Module")
public class WrapModule extends BaseObject {
    protected ModuleEntity module;
    protected boolean registered = false;

    public WrapModule(Environment env, ModuleEntity module) {
        super(env);
        this.module = module;
    }

    public WrapModule(Environment env, ClassEntity clazz) {
        super(env, clazz);
    }

    @Signature({
            @Arg("source"),
            @Arg(value = "compiled", optional = @Optional("false")),
            @Arg(value = "debugInformation", optional = @Optional("true"))
    })
    public Memory __construct(Environment env, Memory... args) throws Throwable {
        InputStream is = Stream.getInputStream(env, args[0]);
        try {
            Context context = new Context(is, Stream.getPath(args[0]), env.getDefaultCharset());
            if (args[1].toBoolean()) {
                ModuleDumper moduleDumper = new ModuleDumper(context, env, args[2].toBoolean());
                module = moduleDumper.load(context.getInputStream(env.getDefaultCharset()));
            } else {
                JvmCompiler compiler = new JvmCompiler(env, context);
                module = compiler.compile(false);
            }
        } finally {
            Stream.closeStream(env, is);
        }
        return Memory.NULL;
    }

    protected void loadModule(Environment env) {
        if (!module.isLoaded()) {
            synchronized (env.scope) {
                if (!module.isLoaded())
                    env.scope.loadModule(module);
            }
        }
    }

    @Signature
    public Memory isRegistered(Environment env, Memory... args) {
        return registered ? Memory.TRUE : Memory.FALSE;
    }

    @Signature
    public Memory register(Environment env, Memory... args) {
        if (registered)
            return Memory.FALSE;

        loadModule(env);
        env.registerModule(module);
        registered = true;
        return Memory.TRUE;
    }

    @Signature(@Arg(value = "variables", type = HintType.ARRAY, optional = @Optional("NULL")))
    public Memory call(Environment env, Memory... args) throws Throwable {
        if (!registered)
            register(env);

        if (args[0].isNull())
            return module.include(env);
        else
            return module.include(env, args[0].toImmutable().toValue(ArrayMemory.class));
    }

    @Signature({
            @Arg("target"),
            @Arg(value = "saveDebugInfo", optional = @Optional("true"))
    })
    public Memory dump(Environment env, Memory... args) throws IOException {
        ModuleDumper moduleDumper = new ModuleDumper(module.getContext(), env, args[1].toBoolean());

        OutputStream os = Stream.getOutputStream(env, args[0]);
        try {
            moduleDumper.save(module, os);
        } finally {
            Stream.closeStream(env, os);
        }
        return Memory.NULL;
    }
}
