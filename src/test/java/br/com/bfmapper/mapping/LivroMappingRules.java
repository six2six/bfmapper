package br.com.bfmapper.mapping;

import br.com.bfmapper.Converter;
import br.com.bfmapper.MappingRules;
import br.com.bfmapper.RulesMapper;
import br.com.bfmapper.model.Livro;
import br.com.bfmapper.model.LivroCanonico;
import br.com.bfmapper.transformer.SimpleTransformer;
import br.com.bfmapper.util.ReflectionUtils;

public class LivroMappingRules implements RulesMapper {

	@Override
	public void loadRules() {
		MappingRules.addRule(new Converter(LivroCanonico.class, Livro.class) {{ 
		    excludeEqualsProperties("anoPublicacao");
		}});
		
	}

	public class StringTransformer implements SimpleTransformer {
		@Override
		public <T> T transform(Object value, Class<T> type) {
			Object returnValue = null;
			
			if (ReflectionUtils.isAssignable(type, String.class)) {
				returnValue = ((String) value).toUpperCase();
			} 
			
			return type.cast(returnValue);
		}
	}
	
}
