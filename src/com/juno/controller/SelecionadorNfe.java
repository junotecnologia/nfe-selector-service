package com.juno.controller;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.filefilter.AgeFileFilter;

import com.juno.exception.SelecionadorNfeException;
import com.juno.model.NfeTransportador;
import com.juno.utils.FileUtils;

public class SelecionadorNfe {
	
	private PropertiesConfiguration config;
	private FileUtils fileUtils = new FileUtils();
	
	private static final String CONFIG_FILE_NAME = "config.properties";
	private static final String CONFIG_FILE_NAME_QUOTED = "\"config.properties\"";
	private static final String DIRETORIO_ORIGEM_FOLDER_PREFIX = "diretorioDeOrigem";
	private static final String PROPERTY_NAME_DIRETORIO_DESTINO = "diretorioDeSaida";
	private static final String PROPERTY_NAME_DATA_MINIMA_ARQUIVO = "dataMinimaDoArquivo";
	private static final String PROPERTY_NAME_CNPJ_TRANSPORTADOR = "cnpjDoTransportador";
	private static final String REGEX_CNPJ_TRANSPORTADOR = "<transporta><CNPJ>(.+?)</CNPJ>";
	private static final Pattern PATTERN_CNPJ_TRANSPORTADOR = Pattern.compile(REGEX_CNPJ_TRANSPORTADOR);
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private static final int IO_ERROR_TOLERANCE_BY_FOLDER = 3;
	
	public void executar() throws Exception {
		NfeTransportador nfeTransportador = carregarDados();
		
		// Valida os dados informados nos campos
		String validacao = validarNfeTransportador(nfeTransportador);
		
		if (validacao.isEmpty()) {
			// Efetua a operação de seleção e cópia
			selecionarNfeTransportador(nfeTransportador);

			// Atualiza quaisquer dados de configuração alterados
			atualizarDados(nfeTransportador);
			
		} else {
			// Exibe mensagem de aviso
			throw new SelecionadorNfeException(validacao);
		}
		config = null;
		fileUtils = null;
		nfeTransportador = null;
	}
	
	/**
	 * Carrega dos dados da aplicação
	 * @throws Exception 
	 */
	protected NfeTransportador carregarDados() throws Exception {
		PropertiesConfiguration config = fileUtils.carregarProperties(CONFIG_FILE_NAME);
		
		NfeTransportador nfeTransportador = new NfeTransportador();
		nfeTransportador.setDiretorioDestino(String.valueOf(config.getProperty(PROPERTY_NAME_DIRETORIO_DESTINO)));
		nfeTransportador.setCnpj(String.valueOf(config.getProperty(PROPERTY_NAME_CNPJ_TRANSPORTADOR)));
		
		Iterator<String> propertyNames = config.getKeys();
		while (propertyNames.hasNext()) {
			String propertyName = propertyNames.next();
			
			if (propertyName != null && propertyName.startsWith(DIRETORIO_ORIGEM_FOLDER_PREFIX)) {
				nfeTransportador.getMapDiretorioOrigem().put(propertyName, String.valueOf(config.getProperty(propertyName)));
			}
		}
		try {
			nfeTransportador.setDataMinimaArquivo(DATE_FORMAT.parse(String.valueOf(config.getProperty(PROPERTY_NAME_DATA_MINIMA_ARQUIVO))));
			
		} catch (Exception e) {
			String erro = "Propriedade \"" + PROPERTY_NAME_DATA_MINIMA_ARQUIVO + "\" do arquivo " + CONFIG_FILE_NAME_QUOTED
					+ " possui valor mal formatado. A data informada nesta propriedade deve ser no formato"
					+ " dd/MM/aaaa HH:mm:ss (dia/mês/ano hora/minuto/segundo). Ex.: 25/04/2017 08:00:00" + System.lineSeparator();
			throw new SelecionadorNfeException(erro);
		}
		this.config = config;
		
		return nfeTransportador;
	}
	
	/**
	 * Valida o preenchimento do objeto NfeTransportador
	 * @param nfeTransportador objeto com CNPJ e diretório de destino
	 * @return String com validação de erro se houver e <b>String vazia</b> se estiver ok
	 * @throws ParseException 
	 */
	protected String validarNfeTransportador(NfeTransportador nfeTransportador) throws ParseException {
		StringBuilder validacao = new StringBuilder();

		// Validando o diretório de destino
		validacao.append(fileUtils.validarDiretorioDestino(nfeTransportador.getDiretorioDestino(), PROPERTY_NAME_DIRETORIO_DESTINO, CONFIG_FILE_NAME_QUOTED));
		
		// Validando os diretórios de origem
		for (Entry<String, String> entryDiretorioOrigem : nfeTransportador.getMapDiretorioOrigem().entrySet()) {
			validacao.append(fileUtils.validarDiretorioOrigem(entryDiretorioOrigem.getValue(), entryDiretorioOrigem.getKey(), CONFIG_FILE_NAME_QUOTED));
		}
		
		// Validando o CNPJ informado
		if (nfeTransportador.getCnpj() == null || nfeTransportador.getCnpj().length() < 14) {
			validacao.append("Propriedade \"" + PROPERTY_NAME_CNPJ_TRANSPORTADOR + "\" não encontrada ou não devidamente preenchida no arquivo "
					+ CONFIG_FILE_NAME_QUOTED + ". Informe o CNPJ do Transportador que será buscado nas NF-es. Digite somente números" + System.lineSeparator());
		} else {
			nfeTransportador.setCnpj(nfeTransportador.getCnpj().replace(".", "").replace("/", "").replace("-", ""));
		}
		
		// Validando se foi informada uma data mínima para os arquivos que serão verificados
		if (nfeTransportador.getDataMinimaArquivo() == null) {
			validacao.append("Propriedade \"" + PROPERTY_NAME_DATA_MINIMA_ARQUIVO + "\" não encontrada ou não devidamente preenchida no arquivo "
					+ CONFIG_FILE_NAME_QUOTED + ". Informe a data mínima para os arquivos que serão verificados" + System.lineSeparator());
		}
		return validacao.toString();
	}
	
	/**
	 * Seleciona e copia os arquivos NF-e referentes ao transportador
	 * encontrados em cada um dos diretórios de origem
	 * @param nfeTransportador objeto com CNPJ e diretório de destino
	 * @throws Exception 
	 */
	protected void selecionarNfeTransportador(NfeTransportador nfeTransportador) throws Exception {
		String diretorioArquivoCopia = fileUtils.getDiretorioArquivoCopia(nfeTransportador.getDiretorioDestino());
		
		Date dataMinimaArquivoNestaBusca = nfeTransportador.getDataMinimaArquivo();
		Date dataInicioDestaBusca = new Date();
		FileFilter fileFilter = new AgeFileFilter(dataMinimaArquivoNestaBusca, false);
		
		for (String pastaAtual : nfeTransportador.getMapDiretorioOrigem().values()) {
			File[] arquivos = new File(pastaAtual).listFiles(fileFilter);
			int qtdeArquivosCopiados = 0;
			int qtdeErros = 0;
	
			for (File arquivo : arquivos) {
				if (arquivo.isFile()) {
					try {
						// Criando a representação do arquivo cópia
						File arquivoCopia = new File(diretorioArquivoCopia + File.separator + arquivo.getName());
						
						if (arquivoCopia.exists()) {
							// Ignora o arquivo se este já existe no diretório de destino
							continue;
						}
						String conteudoArquivo = fileUtils.getConteudoArquivo(new FileInputStream(arquivo));
						
						if (conteudoArquivo == null || conteudoArquivo.isEmpty()) {
							// Ignora o arquivo se estiver vazio
							continue;
						}
						// Procura pelo padrão <transporta><CNPJ>"qualquercoisa"</CNPJ>
						Matcher matcher = PATTERN_CNPJ_TRANSPORTADOR.matcher(conteudoArquivo);
						
						// Verifica se o conteúdo de "qualquercoisa" é igual o CNPJ buscado
						if (matcher.find() && matcher.group(1).equals(nfeTransportador.getCnpj())) {
							
							// Cria o diretório do arquivo cópia caso ainda não tenha sido criado
							fileUtils.criarDiretorio(diretorioArquivoCopia);
							
							// Copia o arquivo ao destino informado
							fileUtils.copiarArquivo(arquivo, arquivoCopia);
							qtdeArquivosCopiados++;
						}
					} catch (FileNotFoundException e) {
						System.err.println("Erro na leitura do arquivo \"" + arquivo.getName() + "\": " + e.getMessage());
						qtdeErros++;
						
					} catch (Exception e) {
						System.err.println("Erro ao copiar o arquivo \"" + arquivo.getName() + "\": " + e.getMessage());
						qtdeErros++;
					}
					
					if (qtdeErros > IO_ERROR_TOLERANCE_BY_FOLDER) {
						// Interrompendo a busca por exceder o limite de tolerância à erros de I/O
						throw new SelecionadorNfeException("Erros encontrados na leitura/cópia de arquivos do diretório " + pastaAtual
								+ "; Verifique as últimas mensagens no log para ver detalhes sobre os erros" + System.lineSeparator());
					}
				}
			}
			
			StringBuilder mensagem = new StringBuilder();
			mensagem.append(qtdeArquivosCopiados);
			mensagem.append(" arquivos NF-e copiados do diretório ");
			mensagem.append(pastaAtual);
			mensagem.append(" em ");
			mensagem.append(DATE_FORMAT.format(new Date()));
			System.out.println(mensagem.toString());
		}
		
		// Atribui a data do início desta execução como a data mínima
		nfeTransportador.setDataMinimaArquivo(dataInicioDestaBusca);
	}
	
	/**
	 * Atualiza os dados da aplicação
	 * @param nfeTransportador
	 * @throws Exception
	 */
	protected void atualizarDados(NfeTransportador nfeTransportador) throws Exception {
		try {
			config.setProperty(PROPERTY_NAME_DATA_MINIMA_ARQUIVO, DATE_FORMAT.format(nfeTransportador.getDataMinimaArquivo()));
			
			// Atualiza o arquivo de configuração se houve alteração
			fileUtils.atualizarProperties(fileUtils.getDiretorioExecucao() + File.separator + CONFIG_FILE_NAME, config);
		
		} catch (Exception e) {
			throw new SelecionadorNfeException("Não foi possível atualizar o arquivo de configuração: " + e.getMessage());
		}
	}
}