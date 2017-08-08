package com.juno;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.juno.controller.SelecionadorNfe;
import com.juno.exception.SelecionadorNfeException;

/**
 * Service de seleção de XML de NF-e pelo CNPJ do transportador declarado
 * @author Juno Tecnologia | suporte@junotecnologia.com
 */
public class NfeSelectorService {
	
	private static final int INTERVALO_EXECUCAO_MILISEGUNDOS = 900000; // = 900 segundos
	private static final int INTERVALO_EXECUCAO_MINUTOS = 15; // Mesmo intervalo que o dado em milisegundos
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	/**
	 * Única instância estática da classe deste serviço
	 */
	private static NfeSelectorService serviceInstance = new NfeSelectorService();

	/**
	 * Método estático chamado pelo prunsrv para iniciar/parar o service.
	 * Passe o argumento "iniciar" para iniciar o service
	 * e passe "parar" para interromper o serviço
	 */
	public static void main(String args[]) {
		String comando = "iniciar";
		if (args.length > 0) {
			comando = args[0];
		}

		if ("iniciar".equals(comando)) {
			serviceInstance.iniciar();
		} else {
			serviceInstance.parar();
		}
	}

	/**
	 * Flag para saber se esta instância do service foi parada
	 */
	private boolean parado = false;

	/**
	 * Inicia a execução deste service
	 */
	private void iniciar() {
		parado = false;
		
		System.out.println("NF-e Selector Service foi iniciado em " + DATE_FORMAT.format(new Date()));
		System.out.println("Intervalo de varredura dos diretórios: " + INTERVALO_EXECUCAO_MINUTOS + " minutos" + System.lineSeparator());

		adiarInicioSeNecessario();

		while (!parado) {
			synchronized (this) {
				try {
					long momentoDoInicio = System.currentTimeMillis();
					
					// Executando a função deste programa
					new SelecionadorNfe().executar();
					System.gc();
					
					long duracaoExecucao = System.currentTimeMillis() - momentoDoInicio;
					System.out.println("Duração desta busca (em milisegundos): " + duracaoExecucao);
					
					if (duracaoExecucao > INTERVALO_EXECUCAO_MILISEGUNDOS) {
						// Aguarda o dobro do tempo para a próxima execução
						System.out.println("A última busca levou mais tempo: " + (duracaoExecucao / 1000)
								+ " segundos. A pŕoxima será executada após o dobro do intervalo padrão");
						this.wait(INTERVALO_EXECUCAO_MILISEGUNDOS * 2);
						
					} else {
						// Aguarda o tempo de intervalo padrão para a próxima execução
						this.wait(INTERVALO_EXECUCAO_MILISEGUNDOS);
					}
				} catch (InterruptedException ie) {
					System.err.println(ie.getLocalizedMessage());

				} catch (SelecionadorNfeException sne) {
					System.err.println(sne.getLocalizedMessage());
					this.parar();
					
				} catch (Exception e) {
					e.printStackTrace();
					this.parar();
				}
			}
		}
		System.out.println("NF-e Selector Service foi encerrado em " + DATE_FORMAT.format(new Date()));
		System.out.println("__________________________________________________________" + System.lineSeparator());
	}

	/**
	 * Encerra a instância deste service
	 */
	private void parar() {
		parado = true;
		
		synchronized (this) {
			this.notify();
		}
	}
	
	/**
	 * Adia a primeira execução de busca caso o instante do início da chamada do
	 * programa não esteja "redondo" com o intervalo de execução dado em minutos
	 */
	private void adiarInicioSeNecessario() {
		Calendar calendar = Calendar.getInstance();
		
		int minutoAtual = calendar.get(Calendar.MINUTE);
		int segundoAtual = calendar.get(Calendar.SECOND);
		
		if (minutoAtual % INTERVALO_EXECUCAO_MINUTOS != 0 || segundoAtual > 5) { // 5 segundos de tolerância
			calendar.add(Calendar.MINUTE, INTERVALO_EXECUCAO_MINUTOS - (minutoAtual % INTERVALO_EXECUCAO_MINUTOS));
			calendar.set(Calendar.SECOND, 0);
			
			try {
				long diferencaEmMilisegundos = calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				System.out.println("Primeira busca será realizada em " + DATE_FORMAT.format(calendar.getTime()));
				
				Thread.sleep(diferencaEmMilisegundos);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}