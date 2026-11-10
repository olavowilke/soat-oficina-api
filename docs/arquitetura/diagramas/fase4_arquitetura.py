"""Arquitetura da Fase 4: três microsserviços, Saga orquestrada e filas SQS.

Gera fase4-arquitetura.png em docs/arquitetura/.
Uso: python docs/arquitetura/diagramas/fase4_arquitetura.py
"""

from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import Lambda
from diagrams.aws.database import Dynamodb, RDSPostgresqlInstance
from diagrams.aws.integration import SQS
from diagrams.aws.network import APIGateway, NLB
from diagrams.k8s.compute import Pod
from diagrams.onprem.client import Client, Users
from diagrams.saas.logging import NewRelic

SAIDA = Path(__file__).resolve().parent.parent / "fase4-arquitetura"

GRAFO = {
    "fontname": "Helvetica",
    "fontsize": "22",
    "bgcolor": "white",
    "splines": "spline",
    "pad": "0.5",
    "nodesep": "0.9",
    "ranksep": "1.6",
    "labelloc": "t",
}
NO = {"fontname": "Helvetica", "fontsize": "12"}
ARESTA = {"fontname": "Helvetica", "fontsize": "10"}

CMD = "#1565c0"
RESP = "#2e7d32"

with Diagram(
    "Fase 4 — microsserviços, Saga orquestrada e filas SQS (homolog)",
    filename=str(SAIDA),
    outformat="png",
    show=False,
    direction="LR",
    graph_attr=GRAFO,
    node_attr=NO,
    edge_attr=ARESTA,
):
    with Cluster("Entrada"):
        cliente = Users("Cliente")
        equipe = Client("Equipe\n(mecânico, admin)")

    with Cluster("Borda AWS"):
        gateway = APIGateway("API Gateway HTTP")
        authorizer = Lambda("Lambda authorizer\n(CPF)")
        nlb = NLB("VPC link + NLB\nlisteners: existente,\n8081, 8082")

    with Cluster("Externos"):
        mp = Client("Mercado Pago\n(sandbox)")
        newrelic = NewRelic("New Relic")

    with Cluster("EKS (cluster único)"):
        with Cluster("namespace oficina"):
            os_svc = Pod("oficina-api (OS)\nNodePort 30080\nSaga orquestradora")
        with Cluster("namespace billing"):
            billing = Pod("billing\nNodePort 30081")
        with Cluster("namespace execucao"):
            execucao = Pod("execution\nNodePort 30082")

    with Cluster("Filas SQS (cada uma com DLQ)"):
        q_bill = SQS("oficina-homolog-\nbilling-commands")
        q_exec = SQS("oficina-homolog-\nexecution-commands")
        q_reply = SQS("oficina-homolog-\nos-saga-replies")
        dlq_bill = SQS("DLQ billing-commands")
        dlq_exec = SQS("DLQ execution-commands")
        dlq_reply = SQS("DLQ os-saga-replies")

    with Cluster("Dados (um banco por serviço)"):
        db_os = RDSPostgresqlInstance("RDS Postgres OS\noficina-homolog")
        db_bill = RDSPostgresqlInstance("RDS Postgres Billing\noficina-homolog-billing")
        db_exec = Dynamodb("DynamoDB\noficina-homolog-execucoes")

    # Entrada
    cliente >> Edge(label="HTTPS") >> gateway
    gateway >> Edge(style="dashed", label="autoriza") >> authorizer
    gateway >> Edge(label="VPC link") >> nlb
    nlb >> Edge(label=":30080") >> os_svc
    nlb >> Edge(label=":8081 → 30081") >> billing
    nlb >> Edge(label=":8082 → 30082") >> execucao
    equipe >> Edge(style="dashed", label="kubectl\nport-forward") >> billing

    # Saga
    os_svc >> Edge(color=CMD, label="comando") >> q_bill >> Edge(color=CMD) >> billing
    os_svc >> Edge(color=CMD, label="comando") >> q_exec >> Edge(color=CMD) >> execucao
    billing >> Edge(color=RESP, label="resposta") >> q_reply
    execucao >> Edge(color=RESP) >> q_reply
    q_reply >> Edge(color=RESP, label="resposta") >> os_svc

    # DLQs
    q_bill >> Edge(style="dotted", color="firebrick", label="falhas") >> dlq_bill
    q_exec >> Edge(style="dotted", color="firebrick") >> dlq_exec
    q_reply >> Edge(style="dotted", color="firebrick") >> dlq_reply

    # Dados
    os_svc >> Edge(label="JDBC") >> db_os
    billing >> Edge(label="JDBC") >> db_bill
    execucao >> Edge(label="SDK") >> db_exec

    # Mercado Pago
    billing >> Edge(label="preferência /\nconsulta / estorno") >> mp
    mp >> Edge(style="dashed", label="webhook\n(rota pública no gateway)") >> gateway

    # Observabilidade
    for svc in (os_svc, billing, execucao):
        svc >> Edge(style="dotted", color="gray") >> newrelic
