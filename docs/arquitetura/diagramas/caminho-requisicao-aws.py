"""Caminho da requisição até a oficina-api na AWS.

Gera caminho-requisicao-aws.png ao lado deste arquivo.
Uso: python docs/arquitetura/diagramas/caminho-requisicao-aws.py
"""

from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import Lambda
from diagrams.aws.database import RDSPostgresqlInstance
from diagrams.aws.network import APIGateway, NLB
from diagrams.k8s.clusterconfig import HPA
from diagrams.k8s.compute import Pod
from diagrams.k8s.network import Service
from diagrams.onprem.client import Client, Users
from diagrams.saas.logging import NewRelic

SAIDA = Path(__file__).with_suffix("")

GRAFO = {
    "fontname": "Helvetica",
    "bgcolor": "white",
    "splines": "spline",
    "pad": "0.5",
    "nodesep": "0.6",
    "labelloc": "t",
    "ranksep": "1.1",
}
NO = {"fontname": "Helvetica", "fontsize": "12"}
ARESTA = {"fontname": "Helvetica", "fontsize": "10"}

with Diagram(
    "oficina-api no caminho da requisição (AWS)",
    filename=str(SAIDA),
    outformat="png",
    show=False,
    direction="LR",
    graph_attr=GRAFO,
    node_attr=NO,
    edge_attr=ARESTA,
):
    with Cluster("Entrada"):
        cliente = Users("Cliente / Oficina")
        externo = Client("Sistema externo\nde orçamento")

    with Cluster("Borda — lambda-auth"):
        gateway = APIGateway("API Gateway HTTP")
        authorizer = Lambda("Lambda authorizer\nJWT HS256 por CPF")
        nlb = NLB("VPC Link + NLB interno")

    with Cluster("EKS — namespace oficina (infra-k8s)"):
        servico = Service("Service oficina-api\nNodePort 30080")
        hpa = HPA("HPA — CPU 60%")
        pods = [Pod("oficina-api #1"), Pod("oficina-api #2")]

    with Cluster("Dependências"):
        banco = RDSPostgresqlInstance("RDS PostgreSQL\ninfra-database")
        smtp = Client("Provedor SMTP")
        newrelic = NewRelic("New Relic")

    cliente >> Edge(label="HTTPS") >> gateway
    gateway >> Edge(style="dashed", label="autoriza") >> authorizer
    gateway >> nlb >> servico
    externo >> Edge(label="webhook\nX-Webhook-Token") >> servico

    servico >> pods
    hpa >> Edge(style="dashed", label="escala") >> pods

    pods[0] >> Edge(label="JDBC") >> banco
    pods[1] >> Edge() >> banco
    pods[0] >> Edge(label="SMTP") >> smtp
    pods[0] >> Edge(label="métricas + logs JSON") >> newrelic
    pods[1] >> Edge() >> newrelic
