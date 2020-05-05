package water.rapids.ast.prims.mungers;

import water.fvec.Frame;
import water.rapids.Env;
import water.rapids.Val;
import water.rapids.ast.AstPrimitive;
import water.rapids.ast.AstRoot;
import water.rapids.vals.ValFrame;

public class AstDropDuplicates extends AstPrimitive<AstDropDuplicates> {
    @Override
    public int nargs() {
        return 1 + 1;
    }

    @Override
    public String[] args() {
        return new String[]{"ary", "cols"};
    }

    @Override
    public Val apply(Env env, Env.StackHelp stk, AstRoot[] asts) {
        final Frame comparedColumns = stk.track(asts[1].exec(env)).getFrame();
        
        
        return new ValFrame(null);
    }

    @Override
    public String str() {
        return "dropdup";
    }
}
