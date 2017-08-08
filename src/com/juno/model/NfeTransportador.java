package com.juno.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NfeTransportador {
	
	private String diretorioDestino;
	private String cnpj;
	private Date dataMinimaArquivo;
	private Map<String, String> mapDiretorioOrigem;
	
	public NfeTransportador() {
		this.mapDiretorioOrigem = new HashMap<>();
	}

	/**
	 * @return the diretorioDestino
	 */
	public String getDiretorioDestino() {
		return diretorioDestino;
	}

	/**
	 * @param diretorioDestino the diretorioDestino to set
	 */
	public void setDiretorioDestino(String diretorioDestino) {
		this.diretorioDestino = diretorioDestino;
	}

	/**
	 * @return the cnpj
	 */
	public String getCnpj() {
		return cnpj;
	}

	/**
	 * @param cnpj the cnpj to set
	 */
	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	/**
	 * @return the dataMinimaArquivo
	 */
	public Date getDataMinimaArquivo() {
		return dataMinimaArquivo;
	}

	/**
	 * @param dataMinimaArquivo the dataMinimaArquivo to set
	 */
	public void setDataMinimaArquivo(Date dataMinimaArquivo) {
		this.dataMinimaArquivo = dataMinimaArquivo;
	}

	/**
	 * @return the mapDiretorioOrigem
	 */
	public Map<String, String> getMapDiretorioOrigem() {
		return mapDiretorioOrigem;
	}

	/**
	 * @param mapDiretorioOrigem the mapDiretorioOrigem to set
	 */
	public void setMapDiretorioOrigem(Map<String, String> mapDiretorioOrigem) {
		this.mapDiretorioOrigem = mapDiretorioOrigem;
	}
}