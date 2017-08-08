package com.juno.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.apache.commons.configuration.PropertiesConfiguration;

public class FileUtils {
	
	private static final String STRING_VAZIA = "";
	private static final DateFormat DATE_FORMAT_ONLY_YEAR = new SimpleDateFormat("yyyy");
	private static final DateFormat DATE_FORMAT_ONLY_MONTH = new SimpleDateFormat("'Mês'-MM");
	private static final DateFormat DATE_FORMAT_ONLY_DAY_OF_MONTH = new SimpleDateFormat("'Dia'-dd");
	
	/**
	 * Retorna o conteúdo do arquivo informado
	 * @author Renan Baggio
	 * @param inputStream do arquivo
	 * @return String com o conteúdo
	 */
	public String getConteudoArquivo(InputStream inputStream) {
		Scanner scanner = null;
		StringBuilder conteudo = new StringBuilder("");
		try {
			scanner = new Scanner(inputStream, "UTF-8");
			while (scanner.hasNextLine()) {
				conteudo.append(scanner.nextLine());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		return conteudo.toString();
	}
	
	/**
	 * Cria o arquivo e pasta no diretório especificado
	 * @author Renan Baggio
	 * @param diretorio da pasta do arquivo
	 * @param nomeArquivo desejado para o arquivo
	 * @return File com caminho completo do novo arquivo
	 * @throws IOException
	 */
	public File criarArquivo(String diretorio, String nomeArquivo) throws IOException {
		criarDiretorio(diretorio);
		return new File(diretorio + nomeArquivo);
	}
	
	/**
	 * Cria o diretório especificado caso ainda não exista
	 * @author Renan Baggio
	 * @param diretorio referente a pasta a ser criada
	 * @throws IOException
	 */
	public void criarDiretorio(String diretorio) throws IOException {
		File file = new File(diretorio);
		
		if (!file.exists()) {
			file.mkdirs();
		}
	}
	
	/**
	 * Salva o inputStream recebido em um arquivo no diretório informado
	 * @author Renan Baggio
	 * @param inputStream para criar o arquivo
	 * @param diretorio para salvar o arquivo
	 * @param nomeArquivo <b>com extenção</b> do arquivo
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void salvarArquivo(InputStream inputStream, String diretorio, String nomeArquivo) throws IOException, FileNotFoundException {
		// Cria o arquivo e o diretório caso o mesmo não exista
		File arquivo = this.criarArquivo(diretorio, nomeArquivo);
		
		// Cria um buffer temporário para o arquivo
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(arquivo));
		
		byte[] temporario = new byte[1024];
		int tamanho;
		
		// Cria o arquivo no diretório
		while ((tamanho = inputStream.read(temporario)) >= 0) {
			bos.write(temporario, 0, tamanho);
		}
		inputStream.close();
		bos.close();
	}
	
	/**
	 * Verifica se o arquivo não existe ou está em uso por outro processo
	 * @author Renan Baggio
	 * @param file Arquivo a ser verificado
	 * @return <b>true</b> se o arquivo não existir ou estiver em uso
	 */
	public boolean isArquivoEmUso(File file) {
		boolean verificadorEspecifico = false;
		
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
			// O channel não será criado se o arquivo não existir ou estiver em uso
			randomAccessFile.getChannel();
			
		} catch (FileNotFoundException fnfe) {
			if (!file.exists()) {
				System.out.println("Arquivo " + file.getName() + " não encontrado");
			} else {
				System.out.println("Arquivo " + file.getName() + " está em uso por outro processo");
				verificadorEspecifico = true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return verificadorEspecifico;
	}
	
	/**
	 * Cria uma cópia do arquivo informado
	 * @author Renan Baggio
	 * @param origem Arquivo a ser copiado
	 * @param destino Pasta destino + nome do arquivo
	 * @throws IOException
	 */
	public void copiarArquivo(File origem, File destino) throws Exception {
		Files.copy(origem.toPath(), destino.toPath()); // TODO Verificar possível memory leak com este método
	}
	
	/**
	 * Obtém o diretório em que o programa foi executado
	 * @author Renan Baggio
	 * @return Diretório onde o programa foi iniciado
	 * @throws Exception
	 */
	public String getDiretorioExecucao() throws Exception {
		String path = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath()).getAbsolutePath();
		return URLDecoder.decode(path, "UTF-8");
	}
	
	/**
	 * Valida se o diretório informado está preenchido, se existe e é uma pasta
	 * @author Renan Baggio
	 * @param folderPath caminho absoluto do diretório a verificar
	 * @param propertyName nome da propriedade de onde foi obtido o diretório
	 * @param configFileNameQuoted nome do arquivo de onde foi obtido o diretório
	 * @return String vazia se estiver tudo ok
	 */
	public String validarDiretorioOrigem(String folderPath, String propertyName, String configFileNameQuoted) {
		if (folderPath == null || folderPath.isEmpty()) {
			return "Propriedade \"" + propertyName 
					+ "\" não encontrada ou não devidamente preenchida no arquivo " + configFileNameQuoted
					+ ". Informe o diretório de ORIGEM que será verificado em busca das NF-es do Transportador"
					+ ". Ex.: F:\\Arquivos\\Notas Fiscais\\GM São Mateus" + System.lineSeparator();
		
		} else if (!new File(folderPath).exists()) {
			return "O diretório de ORIGEM \"" + folderPath 
					+ "\" não existe. Se o caminho estiver correto, crie o diretório e tente novamente" + System.lineSeparator();
		
		} else if (!new File(folderPath).isDirectory()) {
			return "O diretório de ORIGEM \"" + folderPath 
					+ "\" é um arquivo ao invés de uma pasta. Deve ser informado o caminho de uma pasta" + System.lineSeparator();
		}
		return STRING_VAZIA;
	}
	
	/**
	 * Valida se o diretório informado está preenchido, se existe e é uma pasta
	 * @author Renan Baggio
	 * @param folderPath caminho absoluto do diretório a verificar
	 * @param propertyName nome da propriedade de onde foi obtido o diretório
	 * @param configFileNameQuoted nome do arquivo de onde foi obtido o diretório
	 * @return String vazia se estiver tudo ok
	 */
	public String validarDiretorioDestino(String folderPath, String propertyName, String configFileNameQuoted) {
		if (folderPath == null || folderPath.isEmpty()) {
			return "Propriedade \"" + propertyName 
					+ "\" não encontrada ou não devidamente preenchida no arquivo " + configFileNameQuoted
					+ ". Informe o diretório de DESTINO para onde serão copiados os XMLs de NF-es do Transportador"
					+ ". Ex.: F:\\Arquivos\\Notas Fiscais\\Transportadora BR" + System.lineSeparator();
		
		} else if (!new File(folderPath).exists()) {
			return "O diretório de DESTINO \"" + folderPath 
					+ "\" não existe. Se o caminho estiver correto, crie o diretório e tente novamente" + System.lineSeparator();
		
		} else if (!new File(folderPath).isDirectory()) {
			return "O diretório de DESTINO \"" + folderPath 
					+ "\" é um arquivo ao invés de uma pasta. Deve ser informado o caminho de uma pasta" + System.lineSeparator();
		}
		return STRING_VAZIA;
	}
	
	/**
	 * Retorna o path completo do diretório para onde o arquivo será copiado
	 * @author Renan Baggio
	 * @param diretorioDestino Diretório de destino raiz
	 * @return Path completo do diretório para onde o arquivo será copiado
	 */
	public String getDiretorioArquivoCopia(String diretorioDestino) {
		Date dataAtual = new Date();
		return diretorioDestino
				+ File.separator + DATE_FORMAT_ONLY_YEAR.format(dataAtual)
				+ File.separator + DATE_FORMAT_ONLY_MONTH.format(dataAtual)
				+ File.separator + DATE_FORMAT_ONLY_DAY_OF_MONTH.format(dataAtual);
	}
	
	/**
	 * Retorna um objeto PropertiesConfiguration
	 * @author Renan Baggio
	 * @param fileName nome do arquivo properties
	 * @return PropertiesConfiguration
	 * @throws IOException
	 */
	public PropertiesConfiguration carregarProperties(String fileName) throws Exception {
		File file = new File(getDiretorioExecucao() + File.separator + fileName);
		
		if (!file.exists()) {
			throw new Exception("Arquivo de configuração \"" + fileName + "\" não encontrado");
		}
		PropertiesConfiguration properties = new PropertiesConfiguration(file);
		return properties;
	}
	
	/**
	 * Atualiza o arquivo Properties informado
	 * @author Renan Baggio
	 * @param absolutePath caminho até o arquivo properties
	 * @param properties objeto PropertiesConfiguration que será salvo
	 * @throws IOException
	 */
	public void atualizarProperties(String absolutePath, PropertiesConfiguration properties) throws Exception {
		properties.save();
	}
}